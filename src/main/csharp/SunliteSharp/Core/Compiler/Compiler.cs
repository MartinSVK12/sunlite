using SunliteSharp.Core.AST;
using SunliteSharp.Core.Enum;
using SunliteSharp.Core.Modifier;
using SunliteSharp.Runtime;
using SunliteSharp.Util;

namespace SunliteSharp.Core.Compiler;

public class Compiler(Sunlite sl, VM vm, Compiler? enclosing = null): Expr.IVisitor, Stmt.IVisitor
{
    public record Local(Token Name, int depth, bool isCaptured = false)
    {
        public bool IsCaptured { get; set; } = isCaptured;
        public int Depth { get; set; } = depth;
    }

    public record Upvalue(short Index, bool IsLocal);
    
    public class ClassCompiler(ClassCompiler? enclosing)
    {
        public ClassCompiler? Enclosing { get; init; } = enclosing;
        public bool HasSuperclass = false;
    }
    
    private Compiler? Enclosing = enclosing;
    private ClassCompiler? CurrentClass = null;
    
    private string CurrentFile = "<unknown>";
    private FunctionType CurrentFunctionType = FunctionType.Function;
    private MutableChunk Chunk = new MutableChunk([],[],[],new MutableChunkHeader([],""));
    private List<Local> Locals = [];
    private int localsCount = 0;
    private List<Upvalue> Upvalues = [];
    private int LocalScopeDepth = 0;
    private bool TopLevel = false;
    
    private List<int> IncompleteBreaks = [];
    private List<int> IncompleteContinues = [];

    public SLFunction Compile(
        FunctionType type, 
        FunctionModifier modifier, 
        Type returnType, 
        List<Param> parameters, 
        List<Param> typeParams, 
        List<Stmt> statements,
        string path,
        string name = "",
        int arity = 0)
    {
        localsCount = arity;
        CurrentFile = path;
        Chunk.Header.File = CurrentFile;
        CurrentFunctionType = type;

        if (name == "")
        {
            TopLevel = true;
        }
        else
        {
            Chunk.Header.Name = name;
        }

        if (type is FunctionType.Method or FunctionType.Initializer)
        {
            AddIdentifier("this", new Expr.This(Token.Identifier("this", CurrentFile), Type.Unknown));
            Locals.Add(new Local(Token.Identifier("this", CurrentFile), 0));
            localsCount++;
        }
        
        foreach (var statement in statements)
        {
            Compile(statement);
        }

        if (type == FunctionType.Initializer)
        {
            EmitByte(Opcodes.GetLocal, statements.Last());
            EmitShort(0, statements.Last());
            EmitByte(Opcodes.Return, statements.Last());
        }
        else
        {
            EmitByte(Opcodes.Nil,statements.Last());
            EmitByte(Opcodes.Return, statements.Last());
        }

        if (Sunlite.Debug)
        {
            sl.PrintInfo(Disassembler.DisassembleChunk(Chunk.ToImmutable()));
        }
        
        foreach (var incompleteBreak in IncompleteBreaks)
        {
            sl.Error($"Unexpected 'break' outside of loop.", Chunk.Header.Lines[incompleteBreak], Chunk.Header.File);
        }
        foreach (var incompleteContinue in IncompleteContinues)
        {
            sl.Error($"Unexpected 'continue' outside of loop.", Chunk.Header.Lines[incompleteContinue], Chunk.Header.File);
        }

        return new SLFunction(name, returnType, parameters, typeParams, Chunk.ToImmutable(), arity, Upvalues.Count,
            localsCount, modifier);
    }

    private void Compile(Stmt stmt)
    {
        stmt.Accept(this);
    }

    private void Compile(Expr expr)
    {
        expr.Accept(this);
    }
    
    private short AddConstant(AnySLValue value, Element e)
    {
        if (Chunk.Constants.Contains(value))
        {
            return (short) Chunk.Constants.IndexOf(value);
        }
        Chunk.Constants.Add(value);
        var index = Chunk.Constants.Count - 1;
        if (index <= short.MaxValue) return (short) index;
        sl.Error("Too many constants in one chunk.",e.GetLine(), e.GetFile());
        return 0;
    }

    private short AddIdentifier(string str, Element e)
    {
        return AddConstant(new SLString(str), e);
    }
    
    private void EmitByte(byte b, Element expr)
    {
        Chunk.Code.Add((byte)b);
        Chunk.Header.Lines.Add(expr.GetLine());
    }

    private void EmitBytes(byte b1, byte b2, Element expr)
    {
        EmitByte(b1, expr);
        EmitByte(b2, expr);
    }

    private void EmitByte(Opcodes b, Element? expr)
    {
        Chunk.Code.Add((byte)b);
        Chunk.Header.Lines.Add(expr?.GetLine() ?? 0);
    }

    private void EmitShort(short value, Element? expr)
    {
        Chunk.Code.Add((byte)((value >> 8) & 0xFF));
        Chunk.Code.Add((byte)(value & 0xFF));
        var line = expr?.GetLine() ?? 0;
        Chunk.Header.Lines.Add(line);
        Chunk.Header.Lines.Add(line);
    }

    private void EmitBytes(Opcodes b1, Opcodes b2, Element expr)
    {
        EmitByte(b1, expr);
        EmitByte(b2, expr);
    }

    private void EmitBytes(byte b1, Opcodes b2, Element expr)
    {
        EmitByte(b1, expr);
        EmitByte(b2, expr);
    }

    private void EmitBytes(Opcodes b1, byte b2, Element expr)
    {
        EmitByte(b1, expr);
        EmitByte(b2, expr);
    }

    private void EmitBytes(Opcodes b1, byte b2, byte b3, Element expr)
    {
        EmitByte(b1, expr);
        EmitByte(b2, expr);
        EmitByte(b3, expr);
    }

    private void EmitConstant(AnySLValue value, Element expr)
    {
        EmitByte(Opcodes.Constant, expr);
        EmitShort(AddConstant(value, expr), expr);
    }

    private void EmitLoop(int loopStart, Element e)
    {
        EmitByte(Opcodes.Loop, e);
        var offset = Chunk.Length() - loopStart + 2;
        if (offset > short.MaxValue)
        {
            sl.Error("Loop body too large.",e.GetLine(), e.GetFile());
        }

        EmitByte((byte)((offset >> 8) & 0xFF), e);
        EmitByte((byte)(offset & 0xFF), e);
    }

    private short EmitJump(Opcodes opcode, Element e)
    {
        EmitByte(opcode, e);
        EmitByte(0xFF, e);
        EmitByte(0xFF, e);
        return (short) (Chunk.Length() - 2);
    }

    private void PatchJump(short offset, Element e)
    {
        var jump = Chunk.Length() - offset - 2;

        if (jump > short.MaxValue)
        {
            sl.Error( "Too much code to jump over.",e.GetLine(),e.GetFile());
        }

        Chunk.Code[offset] = (byte)((jump >> 8) & 0xFF);
        Chunk.Code[offset + 1] = (byte)(jump & 0xFF);
    }

    private short ResolveLocal(Expr.INamedExpr expr)
    {
        for (var i = 0; i < Locals.Count; i++)
        {
            var local = Locals[i];
            if (local.Name.Lexeme != expr.GetNameToken().Lexeme) continue;
            if (local.Depth == -1)
            {
                sl.Error(expr.GetNameToken(), "Can't read local variable in its own initializer.");
            }
            return (short) i;
        }
        return -1;
    }

    private short ResolveUpvalue(Compiler? compiler, Expr.INamedExpr expr)
    {
        if (compiler == null) return -1;

        var local = compiler.ResolveLocal(expr);
        if (local != -1)
        {
            compiler.Locals[local].IsCaptured = true;
            return AddUpvalue(local, true, expr);
        }

        var upvalue = ResolveUpvalue(compiler.Enclosing, expr);
        if (upvalue != -1)
        {
            return AddUpvalue(upvalue, false, expr);
        }

        return -1;
    }

    private short AddUpvalue(short local, bool isLocal, Expr.INamedExpr expr)
    {
        for (var i = 0; i < Upvalues.Count; i++)
        {
            var upvalue = Upvalues[i];
            if (upvalue.Index == local && upvalue.IsLocal == isLocal)
            {
                return (short)i;
            }
        }

        if (Upvalues.Count >= short.MaxValue)
        {
            sl.Error(expr.GetNameToken(), "Too many upvalues in function.");
            return 0;
        }

        Upvalues.Add(new Upvalue(local, isLocal));
        return (short)(Upvalues.Count - 1);
    }

    private byte ArgumentList(Expr.Call expr)
    {
        foreach (var argument in expr.Arguments)
        {
            Compile(argument);
        }
        return (byte) expr.Arguments.Count;
    }

    private void DefineVariable(short constantIndex, Stmt stmt, bool isVar = false)
    {
        if (LocalScopeDepth > 0)
        {
            if (isVar)
            {
                EmitByte(Opcodes.SetLocal, stmt);
                EmitShort(constantIndex, stmt);
            }
            MarkInitialized();
            return;
        }

        EmitByte(Opcodes.DefGlobal, stmt);
        EmitShort(constantIndex, stmt);
    }

    private void MarkInitialized()
    {
        if (LocalScopeDepth == 0) return;
        Locals[^1].Depth = LocalScopeDepth;
    }

    private short MakeVariable(Token token, Stmt stmt)
    {
        var localIndex = DeclareVariable(token, stmt);
        return LocalScopeDepth > 0 ? localIndex : AddConstant(new SLString(token.Lexeme), stmt);
    }

    private short DeclareVariable(Token token, Stmt stmt)
    {
        if (LocalScopeDepth == 0) return -1;

        foreach (var local in Locals.TakeWhile(local => local.Depth == -1 || local.Depth >= LocalScopeDepth).Where(local => local.Name.Lexeme == token.Lexeme))
        {
            sl.Error(token, "A variable with this name already exists in this scope.");
        }

        return AddLocal(token, stmt);
    }

    private short AddLocal(Token token, Stmt stmt)
    {
        if (Locals.Count >= short.MaxValue)
        {
            sl.Error(token, "Too many local variables in function.");
            return -1;
        }

        Locals.Add(new Local(token, -1));
        localsCount++;
        return (short) (localsCount - 1);
    }

    private void BeginScope(Element e)
    {
        LocalScopeDepth++;
    }

    private void EndScope(Element e)
    {
        LocalScopeDepth--;
        Locals.RemoveAll(local => local.Depth > LocalScopeDepth);
    }

    private void MakeFunction(Stmt.Function stmt)
    {
        var compiler = new Compiler(sl, vm, this);
        compiler.BeginScope(stmt);

        foreach (var constantIndex in stmt.Params.Select(param => compiler.MakeVariable(param.Token, stmt)))
        {
            compiler.DefineVariable(constantIndex, stmt);
        }

        var function = compiler.Compile(
            stmt.Type,
            stmt.Modifier,
            stmt.ReturnType,
            stmt.Params,
            stmt.TypeParams,
            stmt.Body,
            CurrentFile,
            stmt.Name.Lexeme,
            stmt.Params.Count
        );

        EmitByte(Opcodes.Closure, stmt);
        EmitShort(AddConstant(new SLFuncObj(function), stmt), stmt);

        foreach (var upvalue in compiler.Upvalues)
        {
            EmitByte((byte)(upvalue.IsLocal ? 1 : 0), stmt);
            EmitShort(upvalue.Index, stmt);
        }
    }


    public void VisitBinaryExpr(Expr.Binary expr)
    {
        Compile(expr.Left);
        Compile(expr.Right);

        switch (expr.Operator.Type)
        {
            case TokenType.Minus:
                EmitByte(Opcodes.Sub, expr);
                break;
            case TokenType.Slash:
                EmitByte(Opcodes.Divide, expr);
                break;
            case TokenType.Star:
                EmitByte(Opcodes.Multiply, expr);
                break;
            case TokenType.Plus:
                EmitByte(Opcodes.Add, expr);
                break;
            case TokenType.Greater:
                EmitByte(Opcodes.Greater, expr);
                break;
            case TokenType.GreaterEqual:
                EmitBytes(Opcodes.Less, Opcodes.Not, expr);
                break;
            case TokenType.Less:
                EmitByte(Opcodes.Less, expr);
                break;
            case TokenType.LessEqual:
                EmitBytes(Opcodes.Greater, Opcodes.Not, expr);
                break;
            case TokenType.BangEqual:
                EmitBytes(Opcodes.Equal, Opcodes.Not, expr);
                break;
            case TokenType.EqualEqual:
                EmitByte(Opcodes.Equal, expr);
                break;
            case TokenType.Is:
                throw new NotImplementedException("Not yet implemented");
            case TokenType.IsNot:
                throw new NotImplementedException("Not yet implemented");
            default:
                return;
        }
    }

    public void VisitGroupingExpr(Expr.Grouping grouping)
    {
        Compile(grouping.Expression);
    }

    public void VisitUnaryExpr(Expr.Unary unary)
    {
        Compile(unary.Right);

        switch (unary.Operator.Type)
        {
            case TokenType.Minus:
                EmitByte(Opcodes.Negate, unary);
                break;
            
            case TokenType.Bang:
                EmitByte(Opcodes.Not, unary);
                break;
            
            default:
                return;
        }

    }

    public void VisitLiteralExpr(Expr.Literal expr)
    {
        switch (expr.Value)
        {
            case bool b:
                EmitByte(b ? Opcodes.True : Opcodes.False, expr);
                break;

            case double d:
                EmitConstant(new SLNumber(d), expr);
                break;

            case string s:
                EmitConstant(new SLString(s), expr);
                break;

            case null:
                EmitByte(Opcodes.Nil, expr);
                break;

            default:
                return;
        }
    }
    
    public void VisitVariableExpr(Expr.Variable expr)
    {
        Opcodes getOp;

        var arg = ResolveLocal(expr);
        if (arg != -1)
        {
            getOp = Opcodes.GetLocal;
        }
        else
        {
            arg = ResolveUpvalue(enclosing, expr);
            if (arg != -1)
            {
                getOp = Opcodes.GetUpvalue;
            }
            else
            {
                arg = AddIdentifier(expr.Name.Lexeme, expr);
                getOp = Opcodes.GetGlobal;
            }
        }

        EmitByte(getOp, expr);
        EmitShort(arg, expr);

    }

    public void VisitAssignExpr(Expr.Assign expr)
    {
        Opcodes setOp;

        var arg = ResolveLocal(expr);
        if (arg != -1)
        {
            setOp = Opcodes.SetLocal;
        }
        else
        {
            arg = ResolveUpvalue(enclosing, expr);
            if (arg != -1)
            {
                setOp = Opcodes.SetUpvalue;
            }
            else
            {
                arg = AddIdentifier(expr.Name.Lexeme, expr);
                setOp = Opcodes.SetGlobal;
            }
        }

        Compile(expr.Value);
        EmitByte(setOp, expr);
        EmitShort(arg, expr);

    }

    public void VisitLogicalExpr(Expr.Logical expr)
    {
        switch (expr.Operator.Type)
        {
            case TokenType.And:
                Compile(expr.Left);
                var jmp = EmitJump(Opcodes.JumpIfFalse, expr);
                EmitByte(Opcodes.Pop, expr);
                Compile(expr.Right);
                PatchJump(jmp, expr);
                break;

            case TokenType.Or:
                Compile(expr.Left);
                var elseJmp = EmitJump(Opcodes.JumpIfFalse, expr);
                var endJmp = EmitJump(Opcodes.Jump, expr);
                PatchJump(elseJmp, expr);
                EmitByte(Opcodes.Pop, expr);
                Compile(expr.Right);
                PatchJump(endJmp, expr);
                break;

            default:
                return;
        }
    }


    public void VisitCallExpr(Expr.Call expr)
    {
        Compile(expr.Callee);
        var argCount = ArgumentList(expr);
        foreach (var typeArg in expr.TypeArgs)
        {
            EmitConstant(new SLType(typeArg.Type), expr);
        }
        var typeArgCount = (byte) expr.TypeArgs.Count;
        EmitBytes(Opcodes.Call, argCount, typeArgCount, expr);

    }

    public void VisitLambdaExpr(Expr.Lambda expr)
    {
        MakeFunction(expr.Function);
    }

    public void VisitGetExpr(Expr.Get expr)
    {
        Compile(expr.Obj);
        var name = AddIdentifier(expr.Name.Lexeme, expr);
        EmitByte(Opcodes.GetProp, expr);
        EmitShort(name, expr);
    }

    public void VisitArrayGetExpr(Expr.ArrayGet expr)
    {
        Compile(expr.What);
        Compile(expr.Obj);
        EmitByte(Opcodes.ArrayGet, expr);
    }

    public void VisitArraySetExpr(Expr.ArraySet expr)
    {
        Compile(expr.Value);
        Compile(expr.What);
        Compile(expr.Obj);
        EmitByte(Opcodes.ArraySet, expr);
    }

    public void VisitSetExpr(Expr.Set expr)
    {
        var name = AddIdentifier(expr.Name.Lexeme, expr);
        Compile(expr.Obj);
        Compile(expr.Value);
        EmitByte(Opcodes.SetProp, expr);
        EmitShort(name, expr);
    }


    public void VisitThisExpr(Expr.This expr)
    {
        var compiler = this;
        while (compiler != null)
        {
            if (compiler.CurrentClass != null) break;
            compiler = compiler.Enclosing;
        }

        if (compiler == null)
        {
            sl.Error("Can't refer to 'this' outside of a class.", expr.GetLine(), expr.GetFile());
            return;
        }

        Compile(new Expr.Variable(expr.Keyword, Type.Unknown, false));
    }

    public void VisitSuperExpr(Expr.Super expr)
    {
        var compiler = this;
        while (compiler != null)
        {
            if (compiler.CurrentClass != null) break;
            compiler = compiler.Enclosing;
        }

        if (compiler == null)
        {
            sl.Error( "Can't refer to 'super' outside of a class.", expr.GetLine(), expr.GetFile());
            return;
        }
        else if (compiler.CurrentClass?.HasSuperclass == false)
        {
            sl.Error("Can't refer to 'super' in a class with no superclass.", expr.GetLine(), expr.GetFile());
            return;
        }

        Compile(new Expr.Variable(Token.Identifier("this", expr.GetLine(), expr.GetFile()), Type.Unknown, false));
        Compile(new Expr.Variable(Token.Identifier("super", expr.GetLine(), expr.GetFile()), Type.Unknown, false));
        var name = AddIdentifier(expr.Method.Lexeme, expr);
        EmitByte(Opcodes.GetSuper, expr);
        EmitShort(name, expr);
    }


    public void VisitCheckExpr(Expr.Check expr)
    {
        Compile(expr.Left);
        var index = AddConstant(new SLType(expr.Right), expr);
        EmitByte(Opcodes.Check, expr);
        EmitShort(index, expr);
    }

    public void VisitCastExpr(Expr.Cast expr)
    {
        // TODO: Implement cast logic
        Compile(expr.Left);
    }

    public void VisitExprStmt(Stmt.Expression stmt)
    {
        Compile(stmt.Expr);
        EmitByte(Opcodes.Pop, stmt);
    }


    public void VisitVarStmt(Stmt.Var stmt)
    {
        var constantIndex = MakeVariable(stmt.Name, stmt);
        if (stmt.Initializer != null)
        {
            Compile(stmt.Initializer);
        }
        else
        {
            EmitByte(Opcodes.Nil, stmt);
        }

        DefineVariable(constantIndex, stmt, true);
    }


    public void VisitBlockStmt(Stmt.Block stmt)
    {
        BeginScope(stmt);
        foreach (var statement in stmt.Statements)
        {
            Compile(statement);
        }
        EndScope(stmt);
    }

    public void VisitIfStmt(Stmt.If stmt)
    {
        Compile(stmt.Condition);
        var thenJump = EmitJump(Opcodes.JumpIfFalse, stmt);
        EmitByte(Opcodes.Pop, stmt);
        Compile(stmt.ThenBranch);
        var elseJump = EmitJump(Opcodes.Jump, stmt);
        PatchJump(thenJump, stmt);
        EmitByte(Opcodes.Pop, stmt);
        if (stmt.ElseBranch != null)
        {
            Compile(stmt.ElseBranch);
        }
        PatchJump(elseJump, stmt);
    }

    public void VisitWhileStmt(Stmt.While stmt)
    {
        var loopStart = Chunk.Code.Count;
        Compile(stmt.Condition);

        var exitJump = EmitJump(Opcodes.JumpIfFalse, stmt);
        EmitByte(Opcodes.Pop, stmt);

        var body = (Stmt.Block)stmt.Body;
        if (body.Statements.Count > 0)
        {
            Compile(body.Statements[0]);

            if (
                body.Statements.Count == 2 &&
                body.Statements[0] is Stmt.Block &&
                body.Statements[1] is Stmt.Expression exprStmt &&
                exprStmt.Expr is Expr.Assign
            )
            {
                foreach (short cont in IncompleteContinues)
                {
                    PatchJump(cont, stmt);
                }
                IncompleteContinues.Clear();
                Compile(body.Statements[1]);
            }
            else
            {
                List<Stmt> list = body.Statements.GetRange(1, body.Statements.Count - 1);
                foreach (var statement in list)
                {
                    Compile(statement);
                }
                foreach (short cont in IncompleteContinues)
                {
                    PatchJump(cont, stmt);
                }
                IncompleteContinues.Clear();
            }
        }

        EmitLoop(loopStart, stmt);

        PatchJump(exitJump, stmt);
        EmitByte(Opcodes.Pop, stmt);

        foreach (short brk in IncompleteBreaks)
        {
            PatchJump(brk, stmt);
        }
        IncompleteBreaks.Clear();
    }


    public void VisitBreakStmt(Stmt.Break stmt)
    {
        IncompleteBreaks.Add(EmitJump(Opcodes.Jump, stmt));
    }

    public void VisitContinueStmt(Stmt.Continue stmt)
    {
        IncompleteContinues.Add(EmitJump(Opcodes.Jump, stmt));
    }

    public void VisitFunctionStmt(Stmt.Function stmt)
    {
        var constantIndex = AddIdentifier(stmt.Name.Lexeme, stmt);
        MarkInitialized();
        MakeFunction(stmt);
        if (stmt.Type == FunctionType.Function)
        {
            DefineVariable(constantIndex, stmt);
        }
    }


    public void VisitReturnStmt(Stmt.Return stmt)
    {
        if (CurrentFunctionType == FunctionType.Initializer)
        {
            sl.Error( "Can't explicitly return from an initializer.", stmt.GetLine(), stmt.GetFile());
        }

        if (stmt.Value == null)
        {
            EmitByte(Opcodes.Nil, stmt);
            EmitByte(Opcodes.Return, stmt);
        }
        else
        {
            Compile(stmt.Value);
            EmitByte(Opcodes.Return, stmt);
        }
    }


    public void VisitClassStmt(Stmt.Class stmt)
    {
        var className = stmt.Name;
        var nameConstant = AddIdentifier(className.Lexeme, stmt);
        DeclareVariable(className, stmt);

        EmitByte(Opcodes.False, stmt);
        EmitByte(Opcodes.Class, stmt);
        EmitShort(nameConstant, stmt);

        DefineVariable(nameConstant, stmt);

        var classCompiler = new ClassCompiler(CurrentClass);
        CurrentClass = classCompiler;

        if (stmt.Superclass is not null)
        {
            Compile(new Expr.Variable(stmt.Superclass.Name, Type.Unknown, false));

            BeginScope(stmt.Superclass);
            AddLocal(Token.Identifier("super", stmt.GetLine(), stmt.GetFile()), stmt);
            DefineVariable(0, stmt);
            Compile(new Expr.Variable(className, Type.Unknown, false));
            EmitByte(Opcodes.Inherit, stmt.Superclass);
            classCompiler.HasSuperclass = true;
        }

        foreach (var superInterface in stmt.Superinterfaces)
        {
            Compile(new Expr.Variable(superInterface.Name, Type.Unknown, false));
            Compile(new Expr.Variable(className, Type.Unknown, false));
            EmitByte(Opcodes.Inherit, superInterface);
        }

        Compile(new Expr.Variable(className, Type.Unknown, false));

        foreach (var param in stmt.TypeParams)
        {
            var name = AddIdentifier(param.Token.Lexeme, stmt);
            EmitConstant(new SLType(param.Type), stmt);
            EmitByte(Opcodes.TypeParam, stmt);
            EmitShort(name, stmt);
        }

        foreach (var field in stmt.FieldDefaults)
        {
            var name = AddIdentifier(field.Name.Lexeme, stmt);
            if (field.Initializer is not null)
            {
                Compile(field.Initializer);
            }
            EmitConstant(new SLType(field.Type), field);
            if (field.Modifier == FieldModifier.Static || field.Modifier == FieldModifier.StaticConst)
            {
                EmitByte(Opcodes.StaticField, stmt);
            }
            else
            {
                EmitByte(Opcodes.Field, stmt);
            }
            EmitShort(name, stmt);
        }

        foreach (var method in stmt.Methods)
        {
            var methodName = AddIdentifier(method.Name.Lexeme, stmt);
            Compile(method);
            EmitByte(Opcodes.Method, stmt);
            EmitShort(methodName, stmt);
        }

        EmitByte(Opcodes.Pop, stmt);

        if (CurrentClass?.HasSuperclass == true)
        {
            EndScope(stmt);
        }

        CurrentClass = CurrentClass?.Enclosing;
    }


    public void VisitInterfaceStmt(Stmt.Interface stmt)
    {
        var className = stmt.Name;
        var nameConstant = AddIdentifier(className.Lexeme, stmt);
        DeclareVariable(className, stmt);

        EmitByte(Opcodes.True, stmt);
        EmitByte(Opcodes.Class, stmt);
        EmitShort(nameConstant, stmt);

        DefineVariable(nameConstant, stmt);

        var classCompiler = new ClassCompiler(CurrentClass);
        CurrentClass = classCompiler;

        Compile(new Expr.Variable(className, Type.Unknown, false));

        foreach (var method in stmt.Methods)
        {
            var methodName = AddIdentifier(method.Name.Lexeme, stmt);
            Compile(method);
            EmitByte(Opcodes.Method, stmt);
            EmitShort(methodName, stmt);
        }

        EmitByte(Opcodes.Pop, stmt);

        if (CurrentClass?.HasSuperclass == true)
        {
            EndScope(stmt);
        }

        CurrentClass = CurrentClass?.Enclosing;
    }


    public void VisitImportStmt(Stmt.Import stmt)
    {
        
    }

    public void VisitTryCatchStmt(Stmt.TryCatch stmt)
    {
        var tryBegin = Chunk.Code.Count;
        Compile(stmt.TryBody);
        var tryEnd = Chunk.Code.Count;

        var jmp = EmitJump(Opcodes.Jump, stmt);

        var catchBegin = Chunk.Code.Count;
        BeginScope(stmt);
        MakeVariable(stmt.CatchVariable.Token, stmt);
        DefineVariable(0, stmt);
        foreach (var statement in stmt.CatchBody.Statements)
        {
            Compile(statement);
        }
        EndScope(stmt);

        var catchEnd = Chunk.Code.Count;
        PatchJump(jmp, stmt);

        // Assign exception ranges - adjust this line depending on your exception handling structure
        Chunk.Exceptions[new IntRange(tryBegin, tryEnd)] = new IntRange(catchBegin, catchEnd);
    }

    public void VisitThrowStmt(Stmt.Throw stmt)
    {
        Compile(stmt.Expr);
        EmitByte(Opcodes.Throw, stmt);
    }

}