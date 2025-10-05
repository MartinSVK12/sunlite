using SunliteSharp.Core.AST;
using SunliteSharp.Core.Modifier;
using SunliteSharp.Runtime;

namespace SunliteSharp.Core.Compiler;

public class TypeCollector : Stmt.Visitor, Expr.Visitor
{
    
    private readonly Sunlite Sl;

    public TypeCollector(Sunlite sunlite)
    {
        Sl = sunlite;
        TypeScopes.Add(CurrentScope);
        foreach (KeyValuePair<string, AnySLValue> kvp in Sl.Vm!.Globals.Where(kv => kv.Value is SLNativeFuncObj))
        {
            var token = Token.Identifier(kvp.Key, -1, "<global>");
            var nativeFunc = (SLNativeFuncObj)kvp.Value;
            AddFunction(token, [], nativeFunc.Value.ReturnType);
        }
    }
    
    public interface IElementPrototype
    {
        public Type GetElementType();
        public bool IsConstant();
    }

    public record VariablePrototype(Type Type, bool Constant) : IElementPrototype
    {
        public override string ToString()
        {
            return $": {Type}";
        }

        public Type GetElementType() => Type;
        
        public bool IsConstant() => Constant;
    }
    
    public record FunctionPrototype(
        Token Name,
        List<Param> Params,
        Type ReturnType,
        List<Param> TypeParams
    ) : IElementPrototype
    {
        public override string ToString() => $"({string.Join(", ", Params)}): {ReturnType}";

        public Type GetElementType() 
            => TypeParams.Count > 0
                ? Type.OfGenericFunction(Name.Lexeme, ReturnType, Params, TypeParams)
                : Type.OfFunction(Name.Lexeme, ReturnType, Params);

        public bool IsConstant() => false;
    }

    public record Scope(
        Token Name,
        Dictionary<Token, IElementPrototype> Contents,
        int Depth = -1
    )
    {
        public Scope? Outer;
        public List<Scope> Inner = [];

        public override string ToString() => $"scope '{Name.Lexeme}'";
    }
    
    public Dictionary<string, (string, List<string>, List<string>)> TypeHierarchy = new();
    public List<Scope> TypeScopes = [];
    public string CurrentFile = "<unknown>";
    
    public Scope? CurrentScope = new Scope(Token.Identifier("<global>", "<global>"), []);
    public Stmt.Class? CurrentClass = null;
    public Scope? CurrentScopeCandidate = null;
    
    public void AddVariable(Token name, Type type, bool constant = false)
    {
        if (CurrentScope?.Contents.Keys.Any(k => k.Lexeme == name.Lexeme) == true)
            Sl.Error(name, $"Variable '{name.Lexeme}' already declared in this scope.");

        if (CurrentScope is not null) CurrentScope.Contents[name] = new VariablePrototype(type, constant);
    }

    public void AddFunction(Token name, List<Param> parameters, Type returnType, List<Param>? typeParams = null)
    {
        typeParams ??= [];

        if (CurrentScope?.Contents.Keys.Any(k => k.Lexeme == name.Lexeme) == true)
            Sl.Error(name, $"Function '{name.Lexeme}' already declared in this scope.");

        if (CurrentScope is not null) CurrentScope.Contents[name] = new FunctionPrototype(name, parameters, returnType, typeParams);
    }

    public void AddScope(Token name)
    {
        if (CurrentScope is null)
        {
            CurrentScope = new Scope(name, []);
            TypeScopes.Add(CurrentScope);
        }
        else
        {
            var scope = new Scope(name, [], CurrentScope.Depth + 1)
            {
                Outer = CurrentScope
            };
            CurrentScope.Inner.Add(scope);
            CurrentScope = scope;
        }
    }

    public void RemoveScope()
    {
        CurrentScope = CurrentScope?.Outer;
    }

    public void Collect(List<Stmt> statements, string filePath)
    {
        CurrentFile = filePath;
        AddScope(Token.Identifier(filePath,filePath));
        foreach (var statement in statements)
        {
            statement.Accept(this);
        }
        RemoveScope();
    }

    public Scope? GetValidScope(
        Scope? scope,
        Token name,
        Token? enclosing = null,
        int offset = 0,
        Scope? enclosingScope = null)
    {
        var enclosingS = enclosingScope;
        if (scope == null) return null;

        if (scope.Name.Lexeme == enclosing?.Lexeme)
        {
            enclosingS = scope;
        }

        if (scope.Contents.Keys.Any(k => k.Lexeme == name.Lexeme))
        {
            CurrentScopeCandidate = scope;
        }

        if (enclosingS is not null &&
            scope.Depth <= enclosingS.Depth + offset &&
            scope.Contents.Keys.Any(k => k.Lexeme == name.Lexeme))
        {
            return scope;
        }
        else
        {
            foreach (var innerScope in scope.Inner)
            {
                var valid = GetValidScope(innerScope, name, enclosing, offset, enclosingS);
                if (valid is not null) return valid;
            }
            return null;
        }
    }

    public IElementPrototype? FindType(Token name, Token? enclosing = null, int offset = 0)
    {
        CurrentScopeCandidate = null;
        var globalScope = TypeScopes[0];

        if (globalScope.Contents.Keys.Any(k => k.Lexeme == name.Lexeme))
        {
            return globalScope.Contents.First(kv => kv.Key.Lexeme == name.Lexeme).Value;
        }

        var scope = GetValidScope(TypeScopes.FirstOrDefault(), name, enclosing, offset);
        if (scope == null && CurrentScopeCandidate == null) return null;

        if (CurrentScopeCandidate != null)
        {
            return CurrentScopeCandidate.Contents.First(kv => kv.Key.Lexeme == name.Lexeme).Value;
        }

        return scope?.Contents.First(kv => kv.Key.Lexeme == name.Lexeme).Value;
    }


    public void VisitExprStmt(Stmt.Expression stmt)
    {
        stmt.Expr.Accept(this);
    }
    
    public void VisitVarStmt(Stmt.Var stmt)
    {
        AddVariable(stmt.Name, stmt.Type, stmt.Modifier is FieldModifier.Const or FieldModifier.StaticConst);
        stmt.Initializer?.Accept(this);
    }

    public void VisitBlockStmt(Stmt.Block stmt)
    {
        AddScope(Token.Identifier("<block>", stmt.LineNumber, stmt.CurrentFile));
        foreach (var statement in stmt.Statements)
        {
            statement.Accept(this);
        }
        RemoveScope();
    }

    public void VisitIfStmt(Stmt.If stmt)
    {
        stmt.Condition.Accept(this);
        stmt.ThenBranch.Accept(this);
        stmt.ElseBranch?.Accept(this);
    }

    public void VisitWhileStmt(Stmt.While stmt)
    {
        stmt.Condition.Accept(this);
        stmt.Body.Accept(this);
    }

    public void VisitBreakStmt(Stmt.Break stmt)
    {
        // nothing to collect
    }

    public void VisitContinueStmt(Stmt.Continue stmt)
    {
        // nothing to collect
    }

    public void VisitFunctionStmt(Stmt.Function stmt)
    {
        var typeParams = new List<Param>(stmt.TypeParams);
        if (CurrentClass != null)
        {
            typeParams.AddRange(CurrentClass.TypeParams);
        }

        AddFunction(stmt.Name, stmt.Params, stmt.ReturnType, typeParams);
        AddScope(stmt.Name);
        foreach (var param in stmt.Params)
        {
            AddVariable(param.Token, param.Type);
        }
        foreach (var bodyStmt in stmt.Body)
        {
            bodyStmt.Accept(this);
        }
        RemoveScope();
    }

    public void VisitReturnStmt(Stmt.Return stmt)
    {
        stmt.Value?.Accept(this);
    }

    public void VisitClassStmt(Stmt.Class stmt)
    {
        List<Param> classParams = stmt.Methods.FirstOrDefault(m => m.Name.Lexeme == "init")?.Params ?? new List<Param>();
        AddVariable(stmt.Name, Type.OfClass(stmt.Name.Lexeme, classParams));
        AddScope(stmt.Name);
        CurrentClass = stmt;

        var superclass = stmt.Superclass?.Name.Lexeme ?? "<nil>";
        List<string> superinterfaces = stmt.Superinterfaces.Select(i => i.Name.Lexeme).ToList();

        if (TypeHierarchy.ContainsKey(stmt.Name.Lexeme))
        {
            Sl.Error(stmt.Name, $"Class '{stmt.Name.Lexeme}' already defined.");
        }
        TypeHierarchy[stmt.Name.Lexeme] = (superclass, superinterfaces, stmt.TypeParams.Select(tp => tp.Token.Lexeme).ToList());

        if (stmt.Superclass != null)
        {
            AddVariable(Token.Identifier("<superclass>", stmt.Superclass.GetLine(), stmt.Superclass.GetFile()), Type.OfClass(stmt.Superclass.Name.Lexeme,[]));
        }

        foreach (var superinterface in stmt.Superinterfaces)
        {
            AddVariable(Token.Identifier($"<superinterface {superinterface.Name.Lexeme}>", superinterface.GetLine(), superinterface.GetFile()), Type.OfClass(superinterface.Name.Lexeme,[]));
        }

        foreach (var fieldDefault in stmt.FieldDefaults)
        {
            fieldDefault.Accept(this);
        }
        foreach (var method in stmt.Methods)
        {
            method.Accept(this);
        }
        foreach (var typeParam in stmt.TypeParams)
        {
            AddVariable(Token.Identifier($"<{typeParam.Token.Lexeme}>", typeParam.Token.Line, typeParam.Token.File), new Type.Parameter(typeParam.Token));
        }
        CurrentClass = null;
        RemoveScope();
    }

    public void VisitInterfaceStmt(Stmt.Interface stmt)
    {
        AddVariable(stmt.Name, Type.OfClass(stmt.Name.Lexeme,[]));
        List<string> superinterfaces = stmt.Superinterfaces.Select(i => i.Name.Lexeme).ToList();

        if (TypeHierarchy.ContainsKey(stmt.Name.Lexeme))
        {
            Sl.Error(stmt.Name, $"Class '{stmt.Name.Lexeme}' already defined.");
        }
        TypeHierarchy[stmt.Name.Lexeme] = ("<nil>", superinterfaces, stmt.TypeParameters.Select(tp => tp.Token.Lexeme).ToList());

        AddScope(stmt.Name);
        foreach (var method in stmt.Methods)
        {
            method.Accept(this);
        }
        RemoveScope();
    }

    public void VisitImportStmt(Stmt.Import stmt)
    {
        // nothing to collect
    }

    public void VisitTryCatchStmt(Stmt.TryCatch stmt)
    {
        stmt.TryBody.Accept(this);
        stmt.CatchBody.Accept(this);
    }

    public void VisitThrowStmt(Stmt.Throw stmt)
    {
        stmt.Expr.Accept(this);
    }

    public void VisitBinaryExpr(Expr.Binary expr)
    {
        expr.Left.Accept(this);
        expr.Right.Accept(this);
    }

    public void VisitGroupingExpr(Expr.Grouping expr)
    {
        expr.Expression.Accept(this);
    }

    public void VisitUnaryExpr(Expr.Unary expr)
    {
        expr.Right.Accept(this);
    }

    public void VisitLiteralExpr(Expr.Literal expr)
    {
        // nothing to collect
    }

    public void VisitVariableExpr(Expr.Variable expr)
    {
        // nothing to collect
    }

    public void VisitAssignExpr(Expr.Assign expr)
    {
        expr.Value.Accept(this);
    }

    public void VisitLogicalExpr(Expr.Logical expr)
    {
        expr.Left.Accept(this);
        expr.Right.Accept(this);
    }

    public void VisitCallExpr(Expr.Call expr)
    {
        expr.Callee.Accept(this);
        foreach (var argument in expr.Arguments)
        {
            argument.Accept(this);
        }
    }

    public void VisitLambdaExpr(Expr.Lambda expr)
    {
        expr.Function.Accept(this);
    }

    public void VisitGetExpr(Expr.Get expr)
    {
        expr.Obj.Accept(this);
    }

    public void VisitArrayGetExpr(Expr.ArrayGet expr)
    {
        expr.Obj.Accept(this);
        expr.What.Accept(this);
    }

    public void VisitArraySetExpr(Expr.ArraySet expr)
    {
        expr.Obj.Accept(this);
        expr.What.Accept(this);
        expr.Value.Accept(this);
    }

    public void VisitSetExpr(Expr.Set expr)
    {
        expr.Obj.Accept(this);
        expr.Value.Accept(this);
    }

    public void VisitThisExpr(Expr.This expr)
    {
        // nothing to collect
    }

    public void VisitSuperExpr(Expr.Super expr)
    {
        // nothing to collect
    }

    public void VisitCheckExpr(Expr.Check expr)
    {
        expr.Left.Accept(this);
    }

    public void VisitCastExpr(Expr.Cast expr)
    {
        expr.Left.Accept(this);
    }

}