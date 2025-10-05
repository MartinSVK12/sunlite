using System.Data;
using System.Linq.Expressions;
using System.Runtime.InteropServices.ComTypes;
using System.Runtime.InteropServices.JavaScript;
using System.Text;
using SunliteSharp.Core.AST;
using SunliteSharp.Core.Enum;
using SunliteSharp.Core.Modifier;
using SunliteSharp.Runtime;
using static SunliteSharp.Core.Enum.TokenType;

namespace SunliteSharp.Core.Compiler;

public class Parser(List<Token> tokens, Sunlite sl, bool allowImporting = false, bool importing = false, int importingDepth = 0)
{
    private int Current = 0;
    private string CurrentFile = "<unknown>";
    private Token? CurrentClass = null;
    private Token? CurrentFunction = null;
    private int CurrentBlockDepth = 0;
    private int LambdaAmount = 0;

    private class ParseError : Exception;

    public List<Stmt> Parse(string path)
    {
        CurrentFile = path;
        List<Stmt> statements = [];

        while (!IsAtEnd())
        {
            var declaration = Declaration();
            if (declaration is not null) statements.Add(declaration);
        }
        
        return statements;
    }

    private Stmt? Declaration()
    {
        try
        {
            switch (Peek().Type)
            {
                case Var:
                    Advance();
                    return VarDeclaration();
                case Val:
                    Advance();
                    return VarDeclaration(FieldModifier.Const);
                case Fun:
                    Advance();
                    return FuncDeclaration(FunctionType.Function, null);
                case Class:
                    Advance();
                    return ClassDeclaration();
                case Interface:
                    Advance();
                    return InterfaceDeclaration();
                case Import:
                    Advance();
                    return ImportStatement();
                default:
                    return Statement();
            }
        }
        catch (ParseError)
        {
            Synchronize();
            return null;
        }
    }

    private Stmt.Function FuncDeclaration(FunctionType kind, Token? modifier, Token? modifier2 = null)
    {
        if (modifier is not null &&
            !System.Enum.TryParse(typeof(FunctionModifier), modifier.Value.Lexeme, true, out var modifierEnum))
        {
            Error(Peek(), $"Invalid {kind.ToString().ToLower()} modifier '{modifier.Value.Lexeme}'.");
        }
        
        //todo: type parameters
        List<Param> typeParameters = [];

        var funcModifier = FunctionModifierExtensions.Get(modifier, modifier2);
        var signature = FuncSignature(kind);
        
        List<Stmt> body = [];
        if (funcModifier != FunctionModifier.Native && funcModifier != FunctionModifier.StaticNative)
        {
            Consume(LeftBrace, $"Expected '{{' before {kind.ToString().ToLower()} body.");
            body = Block();
        }
        else
        {
            Assert(LeftBrace, $"Native {kind.ToString().ToLower()} cannot have a body.");
        }

        CurrentFunction = null;

        return new Stmt.Function(signature.name, kind, signature.parameters, body, funcModifier, signature.type, typeParameters);
    }
    
    private Stmt.Function AbstractFuncDeclaration()
    {
        Consume(Fun, "Expected abstract method declaration.");
        
        //todo type parameters
        List<Param> typeParameters = [];
        
        var signature = FuncSignature(FunctionType.Method);
        CurrentFunction = null;

        return new Stmt.Function(
            signature.name,
            FunctionType.Method,
            signature.parameters,
            [],
            FunctionModifier.Abstract,
            signature.type,
            typeParameters
        );
    }
    
    private (Token name, List<Param> parameters, Type type) FuncSignature(FunctionType kind)
    {
        var name = 
            kind == FunctionType.Initializer ? 
                Token.Identifier("init", Previous().Line, Previous().File) :
                Consume(Identifier, $"Expected {kind.ToString().ToLower()} name.");

        CurrentFunction = name;

        Consume(LeftParen, $"Expected '(' after {kind.ToString().ToLower()} name.");

        List<Param> parameters = [];

        if (!CheckToken(RightParen))
        {
            do
            {
                if (parameters.Count >= 255)
                {
                    Error(Peek(), "Can't have more than 255 parameters.");
                }

                parameters.Add(new Param(
                    Consume(Identifier, "Expected parameter name."),
                    GetType()
                ));
            } while (Match(Comma));
        }

        Consume(RightParen, "Expected ')' after parameters.");

        var type = GetType(function: true);

        return (name, parameters, type);
    }


    private Stmt.Class ClassDeclaration(ClassModifier modifier = ClassModifier.Normal)
    {
        //todo: type parameters
        List<Param> typeParameters = [];

        var name = Consume(Identifier, "Expected class name.");

        CurrentClass = name;
        
        Expr.Variable? superclass = null;
        if (Match(Extends))
        {
            Consume(Identifier, "Expected superclass name.");
            superclass = new Expr.Variable(Previous(), Type.Unknown, false);
        }
        
        List<Expr.Variable> superinterfaces = [];
        if (Match(Implements))
        {
            do
            {
                if (superinterfaces.Count >= 255)
                {
                    Error(Peek(), "Can't inherit more than 255 superinterfaces.");
                }

                superinterfaces.Add(
                    new Expr.Variable(Consume(Identifier, "Expected superinterface name."),Type.Unknown, false)
                );
            } while (Match(Comma));
        }
        
        Consume(LeftBrace, "Expected '{' before class body.");

        List<Stmt.Function> methods = [];
        List<Stmt.Var> fields = [];
        
        while (!CheckToken(RightBrace) && !IsAtEnd())
        {
            var currentModifier = Peek();

            if (CheckToken(Static) && CheckNext(Native))
            {
                var funcModifier = Peek();
                var funcModifier2 = Next();
                Advance(); // static
                Advance(); // native

                if (Match(Fun))
                {
                    methods.Add(FuncDeclaration(FunctionType.Method, funcModifier, funcModifier2));
                }
            }
            else if (Match(Static) || Match(Native))
            {
                if (Previous().Type == Static)
                {
                    if (Match(Var))
                    {
                        fields.Add(VarDeclaration(FieldModifier.Static));
                    }
                    else if (Match(Val))
                    {
                        fields.Add(VarDeclaration(FieldModifier.StaticConst));
                    }
                    else if (Match(Fun))
                    {
                        methods.Add(FuncDeclaration(FunctionType.Method, currentModifier));
                    }
                    else
                    {
                        throw Error(Peek(), "Expected a field or method declaration.");
                    }
                }
                else if (Match(Fun))
                {
                    methods.Add(FuncDeclaration(FunctionType.Method, currentModifier));
                }
                else
                {
                    throw Error(Peek(), "Expected a field or method declaration.");
                }
            }
            else if (Match(Var))
            {
                fields.Add(VarDeclaration());
            }
            else if (Match(Val))
            {
                fields.Add(VarDeclaration(FieldModifier.Const));
            }
            else if (Match(Fun))
            {
                methods.Add(FuncDeclaration(FunctionType.Method, null));
            }
            else if (Match(Init))
            {
                methods.Add(FuncDeclaration(FunctionType.Initializer, null));
            }
            else
            {
                throw Error(Peek(), "Expected a field or method declaration.");
            }
        }
        
        Consume(RightBrace, "Expected '}' after class body.");

        CurrentClass = null;
        
        return new Stmt.Class(
            name,
            methods,
            fields,
            superclass,
            superinterfaces,
            modifier,
            typeParameters
        );
    }
    
    private Stmt.Interface InterfaceDeclaration()
    {
        List<Param> typeParameters = [];
        if (Match(Less))
        {
            do
            {
                if (typeParameters.Count >= 255)
                {
                    Error(Peek(), "Can't have more than 255 type parameters.");
                }

                var identifier = Consume(Identifier, "Expected type parameter name.");
                typeParameters.Add(new Param(identifier, new Type.Parameter(identifier)));
            } while (Match(Comma));

            Consume(Greater, "Expected '>' after type parameter declaration.");
        }

        var name = Consume(Identifier, "Expected class name.");

        List<Expr.Variable> superinterfaces = [];
        if (Match(Implements))
        {
            do
            {
                if (superinterfaces.Count >= 255)
                {
                    Error(Peek(), "Can't inherit more than 255 superinterfaces.");
                }

                superinterfaces.Add(new Expr.Variable(Consume(Identifier, "Expected superinterface name."), Type.Unknown, false));
            } while (Match(Comma));
        }

        Consume(LeftBrace, "Expected '{' before class body.");

        List<Stmt.Function> methods = [];
        while (!CheckToken(RightBrace) && !IsAtEnd())
        {
            methods.Add(AbstractFuncDeclaration());
        }

        Consume(RightBrace, "Expected '}' after class body.");

        return new Stmt.Interface(name, methods, superinterfaces, typeParameters);

    }

    private Stmt.Import? ImportStatement()
    {
        var keyword = Previous();
        var what = Consume(TokenType.String, "Expected import location string.");
        string importFile = (what.Literal as string)!;
        Consume(Semicolon, "Expected ';' after import statement.");

        if (sl.Collector is null || !allowImporting)
        {
            return null;
        }

        string? data = null;
        List<string> invalidPaths = [];
        string fullPath = "";
        
        foreach (var path in sl.ScriptPaths)
        {
            try
            {
                data = sl.ReadFunction(path + importFile);
                fullPath = path + importFile;
            }
            catch (IOException)
            {
                invalidPaths.Add(path);
            }
        }

        if (data is null || fullPath == "")
        {
            var sb =
                new StringBuilder(
                    $"ImportError: Could not find '{what.Literal}' on the import path list.\nChecked paths:");
            
            foreach (var path in invalidPaths)
            {
                sb.Append($"\t{path}\n");
            }
            sl.Error(keyword, sb.ToString());
            return null;
        }
        
        if (sl.Imports.ContainsKey(fullPath))
        {
            return null;
        }
        
        var scanner = new Scanner(data, sl);
        List<Token> tokens = scanner.ScanTokens(fullPath);
        
        if (sl.HadError)
        {
            sl.Error(keyword, $"ImportError: SyntaxError in file being imported.");
            return null;
        }

        var parser = new Parser(tokens, sl, true, true, importingDepth + 1);
        List<Stmt> statements = parser.Parse(fullPath);
        
        if (sl.HadError)
        {
            sl.Error(keyword, $"ImportError: SyntaxError in file being imported.");
            return null;
        }
        
        sl.Collector.Collect(statements, fullPath);
        
        if (sl.HadError)
        {
            sl.Error(keyword, $"ImportError: TypeError in file being imported.");
            return null;
        }
        
        parser = new Parser(tokens, sl, true, true, importingDepth + 1);
        statements = parser.Parse(fullPath);
        
        if (sl.HadError)
        {
            sl.Error(keyword, $"ImportError: SyntaxError in file being imported.");
            return null;
        }

        var checker = new TypeChecker(sl, sl.Vm);
        checker.Check(statements, fullPath);
        
        if (sl.HadError)
        {
            sl.Error(keyword, $"ImportError: TypeError in file being imported.");
            return null;
        }

        sl.Imports[fullPath] = (importingDepth, statements);

        if (Sunlite.Debug)
        {
            sl.PrintInfo($"Imported '{fullPath}'.");
            sl.PrintInfo();
        }
        
        return new Stmt.Import(keyword, what);
    }

    private Stmt.Var VarDeclaration(FieldModifier modifier = FieldModifier.Normal)
    {
        var name = Consume(Identifier, "Expected variable name.");

        var type = GetType();

        Expr? initializer = null;
        if (Match(Equal)) {
            initializer = Expression();
        }

        Consume(Semicolon, "Expected ';' after variable declaration.");

        return new Stmt.Var(name, type, initializer, modifier);
    }

    private Stmt Statement()
    {
        switch (Peek().Type)
        {
            case Throw:
                Advance();
                return ThrowStatement();
            case LeftBrace:
                Advance();
                return new Stmt.Block(Block(), Previous().Line, Previous().File);
            case If:
                Advance();
                return IfStatement();
            case While:
                Advance();
                return WhileStatement();
            case For:
                Advance();
                return ForStatement();
            case Break:
                Advance();
                return BreakStatement();
            case Continue:
                Advance();
                return ContinueStatement();
            case Return:
                Advance();
                return ReturnStatement();
            case Try:
                Advance();
                return TryCatchStatement();
            default:
                return ExpressionStatement();
        }
    }

    private List<Stmt> Block()
    {
        CurrentBlockDepth++;
        List<Stmt> statements = [];

        while (!CheckToken(RightBrace) && !IsAtEnd())
        {
            var declaration = Declaration();
            if (declaration is not null) statements.Add(declaration);
            else break;
        }
        
        Consume(RightBrace, "Expected '}' after block.");
        CurrentBlockDepth--;
        return statements;
    }
    

    private Stmt.Throw ThrowStatement()
    {
        var value = Expression();
        Consume(Semicolon, "Expected ';' after expression.");
        return new Stmt.Throw(value);
    }
    
    private Stmt.If IfStatement()
    {
        Consume(LeftParen, "Expected '(' after 'if'.");
        var condition = Expression();
        Consume(RightParen, "Expected ')' after 'if' condition.");

        var thenBranch = Statement();
        Stmt elseBranch = null;

        if (Match(Else)) {
            elseBranch = Statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }
    
    private Stmt.While WhileStatement()
    {
        Consume(LeftParen, "Expected '(' after 'while'.");
        var condition = Expression();
        Consume(RightParen, "Expected ')' after 'while' condition.");
        
        var body = Statement();

        return new Stmt.While(condition, body);
    }
    
    private Stmt ForStatement()
    {
        Consume(LeftParen, "Expected '(' after 'for'.");
        
        Stmt? initializer;
        if (Match(Semicolon)) {
            initializer = null;
        } else if (Match(Var)) {
            initializer = VarDeclaration();
        } else {
            initializer = ExpressionStatement();
        }
        
        Expr? condition = null;
        if (!CheckToken(Semicolon)) {
            condition = Expression();
        }
        Consume(Semicolon, "Expected ';' after loop condition.");
        
        Expr? increment = null;
        if (!CheckToken(RightParen)) {
            increment = Expression();
        }
        Consume(RightParen, "Expected ')' after 'for' clauses.");

        var body = Statement();
        
        if (increment is not null) {
            body = new Stmt.Block(
                [
                    body,
                    new Stmt.Expression(increment)
                ],
                Peek().Line,
                Peek().File
            );
        }

        condition ??= new Expr.Literal(true, Peek().Line, Peek().File, Type.Boolean);

        body = new Stmt.While(condition, body);

        if (initializer is not null) {
            body = new Stmt.Block(
                [
                    initializer,
                    body
                ],
                Peek().Line,
                Peek().File
            );
        }

        return body;
    }
    
    private Stmt.Break BreakStatement()
    {
        Consume(Semicolon, "Expected ';' after 'break'.");
        return new Stmt.Break(Previous());
    }
    
    private Stmt.Continue ContinueStatement()
    {
        Consume(Semicolon, "Expected ';' after 'continue'.");
        return new Stmt.Continue(Previous());
    }
    
    private Stmt ReturnStatement()
    {
        var keyword = Previous();
        Expr? value = null;
        if (!CheckToken(Semicolon)) value = Expression();
        Consume(Semicolon, "Expected ';' after return value.");
        return new Stmt.Return(keyword, value);
    }
    
    private Stmt.TryCatch TryCatchStatement()
    {
        var tryToken = Previous();
        Consume(LeftBrace, "Expected '{' before 'try' block.");
        List<Stmt> tryBlock = Block();
        Consume(Catch, "Expected 'catch' after 'try' block.");
        var catchToken = Previous();
        Consume(LeftParen, "Expected '(' after 'catch'.");
        var catchVariable = new Param(Consume(Identifier, "Expected catch variable name."), GetType());
        Consume(RightParen, "Expected ')' after catch variable.");
        Consume(LeftBrace, "Expected '{' before catch block.");
        List<Stmt> catchBlock = Block();
        return new Stmt.TryCatch(tryToken, catchToken, new Stmt.Block(tryBlock, Previous().Line, Previous().File), catchVariable, new Stmt.Block(catchBlock, Previous().Line, Previous().File));
    }
    
    private Stmt.Expression ExpressionStatement()
    {
        var expr = Expression();
        Consume(Semicolon, "Expected ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private Expr Expression()
    {
        return Assignment();
    }

    private Expr Assignment()
    {
        var expr = OrExpr();

        switch (Peek().Type)
        {
            case Equal or PlusEqual or MinusEqual:
                Advance();
                var equals = Previous();
                var value = Assignment();

                switch (expr)
                {
                    case Expr.Variable variable:
                        return variable.Constant ? 
                            throw Error(equals, $"Cannot assign to constant '{variable.Name.Lexeme}'.") : 
                            new Expr.Assign(variable.Name, value, Previous().Type, expr.GetExprType());
                    case Expr.Get get:
                        return get.Constant ? 
                            throw Error(equals, $"Cannot assign to constant '{get.Name.Lexeme}'.") : 
                            new Expr.Set(get.Obj, get.Name, value, Previous().Type, expr.GetExprType());
                    case Expr.ArrayGet arrayGet:
                        return new Expr.ArraySet(arrayGet.Obj, arrayGet.What, value, Previous(), Previous().Type, expr.GetExprType());
                }
                
                Error(equals, "Invalid assignment target.");
                break;
        }
        
        return expr;
    }

    private Expr OrExpr()
    {
        var expr = AndExpr();

        while (Peek().Type == Or)
        {
            Advance();
            var op = Previous();
            var right = AndExpr();
            expr = new Expr.Logical(expr, op, right);
        }
        
        return expr;
    }

    private Expr AndExpr()
    {
        var expr = Check();

        while (Peek().Type == And)
        {
            Advance();
            var op = Previous();
            var right = AndExpr();
            expr = new Expr.Logical(expr, op, right);
        }
        
        return expr;
    }

    private Expr Check()
    {
        var expr = Cast();

        if (!Match(Is) && !Match(IsNot)) return expr;
        var op = Previous();
        var right = GetType(false, true);
        expr = new Expr.Check(expr, op, right);

        return expr;
    }
    
    private Expr Cast()
    {
        var expr = Equality();

        if (!Match(As)) return expr;
        var op = Previous();
        var right = GetType(false, true);
        expr = new Expr.Cast(expr, op, right);

        return expr;
    }
    
    private Expr Equality()
    {
        var expr = ExprComparison();

        while (Match(BangEqual, EqualEqual))
        {
            var op = Previous();
            var right = ExprComparison();
            expr = new Expr.Binary(expr, op, right);
        }
        
        return expr;
    }
    
    private Expr ExprComparison()
    {
        var expr = Term();

        while (Match(Greater, GreaterEqual, Less, LessEqual))
        {
            var op = Previous();
            var right = Term();
            expr = new Expr.Binary(expr, op, right);
        }
        
        return expr;
    }
    
    private Expr Term()
    {
        var expr = Factor();

        while (Match(Minus,Plus))
        {
            var op = Previous();
            var right = Factor();
            expr = new Expr.Binary(expr, op, right);
        }
        
        return expr;
    }
    
    private Expr Factor()
    {
        var expr = Unary();

        while (Match(Slash, Star))
        {
            var op = Previous();
            var right = Unary();
            expr = new Expr.Binary(expr, op, right);
        }
        
        return expr;
    }
    
    private Expr Unary()
    {
        while (Match(Bang, Minus))
        {
            var op = Previous();
            var right = Unary();
            return new Expr.Unary(op, right);
        }
        
        return Lambda();
    }

    private Expr Lambda()
    {
        var token = Peek();
        if (!Match(Fun)) return Call();
        
        var name = $"<lambda {LambdaAmount}>";
        LambdaAmount++;
        CurrentFunction = Token.Identifier(name, Previous().Line, Previous().File);

        List<Param> typeParameters = [];

        if (Match(Less))
        {
            do
            {
                if (typeParameters.Count >= 255)
                {
                    Error(Peek(), "Can't have more than 255 type parameters.");
                }

                Token identifier = Consume(Identifier, "Expected type parameter name.");
                typeParameters.Add(new Param(identifier, new Type.Parameter(identifier)));
            } 
            while (Match(Comma));

            Consume(Greater, "Expected '>' after type parameter declaration.");
        }

            
        Consume(LeftParen, "Expected '(' after lambda expression.");
        return FinishLambda(token, name, typeParameters);
    }

    private Expr.Lambda FinishLambda(Token token, string name, List<Param> typeParams)
    {
        List<Param> parameters = [];

        if (!CheckToken(RightParen)) {
            do {
                if (parameters.Count >= 255) {
                    Error(Peek(), "Can't have more than 255 parameters.");
                }

                parameters.Add(new Param(
                    Consume(Identifier, "Expected parameter name."),
                    GetType()
                ));
            } while (Match(Comma));
        }

        Consume(RightParen, "Expected ')' after parameters.");

        var returnType = GetType(function: true);

        Consume(LeftBrace, "Expected '{' before lambda body.");
        List<Stmt> body = Block();

        CurrentFunction = null;

        return new Expr.Lambda(
            new Stmt.Function(
                new Token(
                    Identifier,
                    name,
                    null,
                    token.Line,
                    CurrentFile,
                    token.Pos
                ),
                FunctionType.Lambda,
                parameters,
                body,
                FunctionModifier.Normal,
                returnType,
                typeParams
            )
        );

    }
    
    private Expr Call()
    {
        var expr = Primary();

        while (true)
        {
            if (Match(LeftParen))
            {
                List<Param> typeParameters = [];
                
                if (Match(Less))
                {
                    var i = 0;
                    do
                    {
                        if (typeParameters.Count >= 255)
                        {
                            Error(Peek(), "Can't have more than 255 type parameters.");
                        }

                        if (sl.Collector != null && expr is Expr.NamedExpr namedExpr)
                        {
                            var typeParams = sl.Collector.TypeHierarchy.TryGetValue(namedExpr.GetNameToken().Lexeme, out var tuple)
                                ? tuple.Item3
                                : null;

                            var paramName = typeParams != null && i < typeParams.Count ? typeParams[i] : "?";
                            typeParameters.Add(new Param(Token.Identifier(paramName, Peek().File), GetType(function: false, noColon: true)));
                        }
                        else
                        {
                            typeParameters.Add(new Param(Token.Identifier("?", Peek().File), GetType(function: false, noColon: true)));
                        }

                        i++;
                    } while (Match(Comma));

                    Consume(Greater, "Expected '>' after type parameter declaration.");
                }

                
                if(expr is Expr.GenericExpr genericExpr)
                {
                    typeParameters.AddRange(genericExpr.GetTypeArguments());
                }

                expr = FinishCall(expr, typeParameters);
            } 
            else if (Match(Dot))
            {
                var name = Consume(Identifier, "Expected expression after '.'.");
                
                if (sl.Collector != null)
                {
                    var resolvedType = sl.Collector.FindType(
                        name,
                        Token.Identifier(expr.GetExprType().Name(), -1, CurrentFile)
                    );

                    expr = new Expr.Get(
                        expr,
                        name,
                        resolvedType?.GetElementType() ?? Type.Unknown,
                        resolvedType?.IsConstant() ?? false, []
                    );
                }
                else
                {
                    expr = new Expr.Get(expr, name, Type.Unknown, false, []);
                }
                
            } 
            else if (Match(LeftBracket))
            {
                var index = Expression();
                Consume(RightBracket, "Expected ']' after expression.");
                expr = new Expr.ArrayGet(expr, index, Previous());

            }
            else
            {
                break;
            }
        }
        
        return expr;
    }
    
    private Expr.Call FinishCall(Expr callee, List<Param> typeArguments)
    {
        List<Expr> arguments = [];

        if (!CheckToken(RightParen))
        {
            do
            {
                if (arguments.Count >= 255)
                {
                    Error(Peek(), "Can't have more than 255 arguments.");
                }

                arguments.Add(Expression());
            } while (Match(Comma));
        }

        var paren = Consume(RightParen, "Expected ')' after arguments.");

        return new Expr.Call(callee, paren, arguments, typeArguments);
    }


    private Expr Primary()
    {
        switch (Peek().Type)
        {
            case False:
                Advance();
                return new Expr.Literal(false, Previous().Line, Previous().File, Type.Boolean);
            case True:
                Advance();
                return new Expr.Literal(true, Previous().Line, Previous().File, Type.Boolean);
            case TokenType.Nil:
                Advance();
                return new Expr.Literal(null, Previous().Line, Previous().File, Type.Nil);
            case Number or TokenType.String:
                Advance();
                return new Expr.Literal(Previous().Literal, Previous().Line, Previous().File, Previous().Type == Number ? Type.Number : Type.String);
            case LeftParen:
                Advance();
                var expr = Expression();
                Consume(RightParen, "Expected ')' after expression.");
                return new Expr.Grouping(expr);
            case This:
                Advance();
                if (sl.Collector is not null && CurrentClass is not null)
                {
                    var collector = sl.Collector;
                    var globalToken = Token.Identifier("<global>", -1, CurrentFile);

                    var type = collector.FindType((Token)CurrentClass, globalToken);

                    var scope = collector.GetValidScope(
                        collector.TypeScopes.First(),
                        (Token)CurrentClass,
                        globalToken
                    )?.Inner.FirstOrDefault(s => s.Name.Lexeme == CurrentClass.Value.Lexeme);

                    var typeParams = scope?.Contents.Keys
                        .Where(t => t.Lexeme.StartsWith('<'))
                        .ToList();

                    if (typeParams is { Count: > 0 })
                    {
                        var baseGenericType = Type.OfGenericObject(
                            scope.Name.Lexeme,
                            typeParams.Select(tp => new Param(Token.Identifier(tp.Lexeme.Trim('<', '>'),CurrentFile), Type.NullableAny)).ToList()
                        );

                        return new Expr.This(Previous(), baseGenericType);
                    }

                    var returnType = (type?.GetElementType() as Type.Reference)?.ReturnType ?? Type.Unknown;
                    return new Expr.This(Previous(), returnType);
                }

                return new Expr.This(Previous(), Type.Unknown);
            case Super:
                Advance();
                var keyword = Previous();
                Consume(Dot, "Expected '.' after 'super'.");
                if (!Match(Identifier, Init))
                {
                    throw Error(Peek(), "Expected superclass method name.");
                }
                var method = Previous();
                if (sl.Collector != null)
                {
                    var type = sl.Collector.FindType(
                        Token.Identifier("<superclass>", -1, keyword.File),
                        CurrentClass
                    )?.GetElementType() ?? Type.Unknown;

                    var methodType = sl.Collector.FindType(
                        Token.Identifier(method.Lexeme, -1, keyword.File),
                        Token.Identifier(type.Name(), -1, keyword.File)
                    )?.GetElementType() ?? Type.Unknown;

                    return new Expr.Super(keyword, method, methodType);
                }
                else
                {
                    return new Expr.Super(keyword, method, Type.Unknown);
                }

            case Identifier:
                Advance();
                var varToken = Previous();
                if (sl.Collector != null)
                {
                    var type = sl.Collector.FindType(
                        varToken,
                        CurrentFunction ?? Token.Identifier("<global>", CurrentFile),
                        CurrentBlockDepth
                    );

                    return new Expr.Variable(
                        varToken,
                        type?.GetElementType() ?? Type.Unknown,
                        type?.IsConstant() ?? false
                    );
                }

                return new Expr.Variable(varToken, Type.Unknown, false);
        }
        
        throw Error(Peek(), "Expected expression.");
    }

    private Type GetType(bool function = false, bool noColon = false)
    {
        Type  type = function ? Type.Nil : Type.Any;
        if (!Match(Colon) && !noColon) return type;
        List<TypeToken> typeTokens = getTypeTokens();
        type = Type.Of(typeTokens, sl);
        return type;
    }

    private List<TypeToken> getTypeTokens(bool insideUnion = false)
    {
        var mainToken = Peek();
        if (!Match(TypeBoolean, TypeString, TypeNumber, TypeFunction, TypeClass, TypeAny, TypeArray, TypeTable,
                TypeGeneric, Identifier, TypeNil))
        {
            throw Error(mainToken, "Expected type.");
        }

        List<TypeToken> types = [];
        Dictionary<Token, List<TypeToken>> unionTypes = [];
        List<TypeToken> typeParameters = [];

        if (Match(Less))
        {
            do
            {
                if (typeParameters.Count >= 255)
                {
                    Error(Peek(), "Can't have more than 255 type parameters.");
                }
                
                var typeParamToken = Peek();
                if (!CheckTokens(TypeBoolean, TypeString, TypeNumber, TypeFunction, TypeClass, TypeAny, TypeArray, TypeTable,
                        TypeGeneric, Identifier, TypeNil))
                {
                    throw Error(mainToken, "Expected type for type parameter.");
                }
                typeParameters.AddRange(getTypeTokens());
                
            } while (Match(Comma));
            Consume(Greater, "Expected '>' after type parameters.");
        }

        if (CheckToken(Pipe) && !insideUnion)
        {
            Advance();
            do
            {
                if (unionTypes.Count >= 255)
                {
                    Error(Peek(), "Can't have more than 255 types in a union.");
                }

                var unionMemberToken = Peek();
                if (!CheckTokens(TypeBoolean, TypeString, TypeNumber, TypeFunction, TypeClass, TypeAny, TypeArray, TypeTable,
                        TypeGeneric, Identifier, TypeNil))
                {
                    throw Error(mainToken, "Expected type after '|'.");
                }
                unionTypes[unionMemberToken] = getTypeTokens(true);

            } while (Match(Pipe));
        }

        if (Match(Question))
        {
            var token = Previous();
            unionTypes[token] = [
                new TypeToken(
                    new Dictionary<Token, List<TypeToken>>
                    {
                        [token] = [new TypeToken(new Dictionary<Token, List<TypeToken>>
                        {
                            [token] = []
                        },typeParameters)]
                    },typeParameters)
            ];
        }

        unionTypes[mainToken] = [
            new TypeToken(
                new Dictionary<Token, List<TypeToken>>
                {
                    [mainToken] = [new TypeToken(new Dictionary<Token, List<TypeToken>>
                    {
                        [mainToken] = []
                    },typeParameters)]
                },typeParameters)
        ];
        
        types.Add(new TypeToken(unionTypes, typeParameters));

        return types;
    }

    private bool IsAtEnd()
    {
        return Peek().Type == Eof;
    }

    private Token Peek()
    {
        return tokens[Current];
    }

    private Token Previous()
    {
        return tokens[Current - 1];
    }

    private Token Next()
    {
        return Current + 1 >= tokens.Count ? throw Error(Peek(), "Unexpected end of file.") : tokens[Current + 1];
    }
    
    private Token Advance()
    {
        if(!IsAtEnd()) Current++;
        return Previous();
    }

    private bool CheckNext(TokenType type)
    {
        if(IsAtEnd()) return false;
        return Next().Type == type;
    }

    private bool CheckNext(params TokenType[] types)
    {
        return types.Any(CheckNext);
    }
    
    private bool CheckToken(TokenType type)
    {
        if(IsAtEnd()) return false;
        return Peek().Type == type;
    }

    private bool CheckTokens(params TokenType[] types)
    {
        return types.Any(CheckToken);
    }

    private bool Match(params TokenType[] types)
    {
        foreach (var type in types)
        {
            if (CheckToken(type))
            {
                Advance();
                return true;
            }
{}      }
        return false;
    }

    private Token Assert(TokenType type, string message)
    {
        return !CheckToken(type) ? Peek() : throw Error(Peek(), message);
    }
    
    private Token Consume(TokenType type, string message)
    {
        return CheckToken(type) ? Advance() : throw Error(Peek(), message);
    }

    private ParseError Error(Token token, string message)
    {
        sl.Error(token, message);
        return new ParseError();
    }
    
    private void Synchronize()
    {
        Advance();

        while (!IsAtEnd())
        {
            if(Previous().Type == Semicolon) return;

            switch (Peek().Type)
            {
                case Class or Interface or For or Var or Val or If or While or Return or Break or Continue:
                    return;
            }
            
            Advance();
        }
    }

}