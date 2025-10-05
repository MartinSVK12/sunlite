using SunliteSharp.Core.AST;
using SunliteSharp.Core.Enum;
using SunliteSharp.Runtime;

namespace SunliteSharp.Core.Compiler;

public class TypeChecker(Sunlite sl, VM? vm): Expr.Visitor, Stmt.Visitor
{
    private string CurrentFile = "";
    private Stack<Stmt.NamedStmt> Scopes = new();

    public void CheckType(Type expected, Type actual, bool runtime = false, Token? token = null)
    {
        if (Sunlite.Debug)
        {
            sl.PrintInfo($"Checking if type '{expected}' matches '{actual}'");
        }
        
        var valid = Type.Contains(actual, expected, sl);
        if (valid) return;
        if (runtime && vm is not null)
        {
            vm.ThrowException(vm.FrameStack.Count-1, new SLString($"TypeError: Expected '{expected}' but got '{actual}'."));
        }
        else
        {
            sl.Error(token!.Value, $"Expected '{expected}' but got '{actual}'.");
        }
    }

    public void Check(List<Stmt> statements, string file)
    {
        CurrentFile = file;
        foreach (var statement in statements)
        {
            Check(statement);
        }
    }

    public void Check(Stmt stmt)
    {
        stmt.Accept(this);
    }
    
    public void Check(Expr expr)
    {
        expr.Accept(this);
    }

    public void VisitBinaryExpr(Expr.Binary expr)
    {
        Check(expr.Left);
        Check(expr.Right);
    }

    public void VisitGroupingExpr(Expr.Grouping expr)
    {
        Check(expr.Expression);
    }

    public void VisitUnaryExpr(Expr.Unary expr)
    {
        Check(expr.Right);
    }

    public void VisitLiteralExpr(Expr.Literal expr)
    {

    }

    public void VisitVariableExpr(Expr.Variable expr)
    {

    }

    public void VisitAssignExpr(Expr.Assign expr)
    {
        Check(expr.Value);
        CheckType(expr.GetExprType(), expr.Value.GetExprType(), false, expr.Name);
    }

    public void VisitLogicalExpr(Expr.Logical expr)
    {
        Check(expr.Left);
        Check(expr.Right);
    }

    public void VisitCallExpr(Expr.Call expr)
    {
        Check(expr.Callee);
        foreach (var arg in expr.Arguments)
        {
            Check(arg);
        }
        
        var calleeType = expr.Callee.GetExprType();
        if (calleeType is Type.Reference reference)
        {
            if (reference.Type is PrimitiveType.Function or PrimitiveType.Class)
            {
                var typeArgs = expr.TypeArgs.ToList();
                var parameters = reference.Params;
                var i = 0;
                foreach (var paramArg in expr.Arguments.Zip(parameters))
                {
                    if (paramArg.Second.Type is Type.Parameter parameter)
                    {
                        if (typeArgs.Count == 0)
                        {
                            sl.Warn(expr.Paren, $"Cannot determine concrete type of '{parameter.Name()}'.");
                            continue;
                        }
                        CheckType(typeArgs[i].Type, paramArg.First.GetExprType(), false, expr.Paren);
                        i++;
                    }
                    else
                    {
                        CheckType(paramArg.Second.Type, paramArg.First.GetExprType(), false, expr.Paren);   
                    }
                }
            }
        }
    }

    public void VisitLambdaExpr(Expr.Lambda expr)
    {
        VisitFunctionStmt(expr.Function);
    }

    public void VisitGetExpr(Expr.Get expr)
    {
        Check(expr.Obj);
    }

    public void VisitArrayGetExpr(Expr.ArrayGet expr)
    {
        Check(expr.Obj);
        Check(expr.What);
        Type indexType = Type.Number;
        if (expr.Obj.GetExprType() is Type.Reference { Type: PrimitiveType.Table } reference)
        {
            indexType = reference.TypeParams[0].Type;
        }
        CheckType(indexType, expr.What.GetExprType(), false, expr.Token);
    }

    public void VisitArraySetExpr(Expr.ArraySet expr)
    {
        Check(expr.Obj);
        Check(expr.What);
        Check(expr.Value);
        var exprType = expr.GetExprType();
        Type indexType = Type.Number;
        if (expr.Obj.GetExprType() is Type.Reference { Type: PrimitiveType.Table } reference)
        {
            indexType = reference.TypeParams[0].Type;
        }
        CheckType(indexType, expr.What.GetExprType(), false, expr.Token);
        CheckType(exprType, expr.Value.GetExprType(), false, expr.Token);
    }

    public void VisitSetExpr(Expr.Set expr)
    {
        Check(expr.Obj);
        Check(expr.Value);
        var exprType = expr.GetExprType();
        if (exprType is Type.Parameter parameter && expr.Obj.GetExprType() is Type.Reference { Type: PrimitiveType.Object } reference)
        {
            var param = reference.TypeParams.Find(p => p.Token.Lexeme == parameter.Name());
            if (param is not null)
            {
                exprType = param.Type; 
            }
        }
        CheckType(exprType, expr.Value.GetExprType(), false, expr.GetNameToken());
    }

    public void VisitThisExpr(Expr.This expr)
    {

    }

    public void VisitSuperExpr(Expr.Super expr)
    {

    }

    public void VisitCheckExpr(Expr.Check expr)
    {
        Check(expr.Left);
    }

    public void VisitCastExpr(Expr.Cast expr)
    {
        Check(expr.Left);
    }

    public void VisitExprStmt(Stmt.Expression stmt)
    {
        Check(stmt.Expr);
    }

    public void VisitVarStmt(Stmt.Var stmt)
    {
        if (stmt.Initializer is null) return;
        Check(stmt.Initializer);
        CheckType(stmt.Type, stmt.Initializer.GetExprType(), false, stmt.Name);
    }

    public void VisitBlockStmt(Stmt.Block stmt)
    {
        Scopes.Push(stmt);
        Check(stmt.Statements, stmt.GetFile());
        Scopes.Pop();
    }

    public void VisitIfStmt(Stmt.If stmt)
    {
        Check(stmt.ThenBranch);
        if (stmt.ElseBranch is not null) Check(stmt.ElseBranch);
    }

    public void VisitWhileStmt(Stmt.While stmt)
    {
        Check(stmt.Body);   
    }

    public void VisitBreakStmt(Stmt.Break stmt)
    {

    }

    public void VisitContinueStmt(Stmt.Continue stmt)
    {

    }

    public void VisitFunctionStmt(Stmt.Function stmt)
    {
        Scopes.Push(stmt);
        Check(stmt.Body, stmt.GetFile());
        Scopes.Pop();
    }

    public void VisitReturnStmt(Stmt.Return stmt)
    {
        if (stmt.Value is null) return;
        Check(stmt.Value);
        if (Scopes.Count == 0) return;
        var enclosing = Scopes.Peek();
        if (enclosing is Stmt.Function function)
        {
            CheckType(function.ReturnType, stmt.Value.GetExprType(), false, stmt.Keyword);
        }
    }

    public void VisitClassStmt(Stmt.Class stmt)
    {
        Scopes.Push(stmt);
        Check([..stmt.Methods], stmt.GetFile());
        Check([..stmt.FieldDefaults], stmt.GetFile());
        Scopes.Pop();
    }

    public void VisitInterfaceStmt(Stmt.Interface stmt)
    {
        Scopes.Push(stmt);
        Check([..stmt.Methods], stmt.GetFile());
        Scopes.Pop();
    }

    public void VisitImportStmt(Stmt.Import stmt)
    {

    }

    public void VisitTryCatchStmt(Stmt.TryCatch stmt)
    {
        Check(stmt.TryBody);
        Check(stmt.CatchBody);
    }

    public void VisitThrowStmt(Stmt.Throw stmt)
    {

    }
}