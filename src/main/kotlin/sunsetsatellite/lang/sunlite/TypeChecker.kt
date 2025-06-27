package sunsetsatellite.lang.sunlite

import com.sun.beans.TypeResolver
import sunsetsatellite.lang.sunlite.TypeCollector.Scope
import sunsetsatellite.vm.sunlite.SLString
import sunsetsatellite.vm.sunlite.VM
import java.util.Stack
import kotlin.math.exp

class TypeChecker(val sunlite: Sunlite, val vm: VM?): Expr.Visitor<Unit>, Stmt.Visitor<Unit> {

    val scopes: Stack<Stmt.NamedStmt> = Stack()
    var path: Token? = null

    fun check(statements: List<Stmt>, path: String? = null){
        path?.let { this.path = Token.identifier(it,-1,it) }
        statements.forEach { it.accept(this) }
    }

    fun check(stmt: Stmt){
        stmt.accept(this)
    }

    fun check(expr: Expr){
        expr.accept(this)
    }

    fun checkType(
        expected: Type,
        actual: Type,
        token: Token,
        runtime: Boolean = false
    ) {
        if(Sunlite.debug){
            println("Checking if type '$expected' matches '$actual' at '${token.lexeme}'")
        }
        val valid = Type.contains(actual, expected)
        if(!valid){
            if(runtime && vm != null){
                vm.throwException(0, SLString("Expected '$expected' but got '$actual'."))
            } else {
                sunlite.error(token, "Expected '$expected' but got '$actual'.")
            }
            return
        }
    }

    override fun visitBinaryExpr(expr: Expr.Binary) {
        check(expr.left)
        check(expr.right)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping) {
       check(expr.expression)
    }

    override fun visitUnaryExpr(expr: Expr.Unary) {
       check(expr.right)
    }

    override fun visitLiteralExpr(expr: Expr.Literal) {
       
    }

    override fun visitVariableExpr(expr: Expr.Variable) {
       
    }

    override fun visitAssignExpr(expr: Expr.Assign) {
        check(expr.value)
        checkType(expr.getExprType(), expr.value.getExprType(),expr.name, false)
    }

    override fun visitLogicalExpr(expr: Expr.Logical) {
        check(expr.left)
        check(expr.right)
    }

    override fun visitCallExpr(expr: Expr.Call) {
        check(expr.callee)
        expr.arguments.forEach { check(it) }

        val calleeType = expr.callee.getExprType()
        if(calleeType is Type.Reference){
            if(calleeType.type == PrimitiveType.FUNCTION){
                val params = calleeType.params
                expr.arguments.zip(params).forEach {
                    checkType(it.second.type, it.first.getExprType(), expr.paren, false)
                }
            }
        }

    }

    override fun visitLambdaExpr(expr: Expr.Lambda) {
        visitFunctionStmt(expr.function)
    }

    override fun visitGetExpr(expr: Expr.Get) {
        check(expr.obj)
    }

    override fun visitArrayGetExpr(expr: Expr.ArrayGet) {
       
    }

    override fun visitArraySetExpr(expr: Expr.ArraySet) {
       
    }

    override fun visitSetExpr(expr: Expr.Set) {
        check(expr.obj)
        check(expr.value)
        checkType(expr.getExprType(),expr.value.getExprType(),expr.name, false)
    }

    override fun visitThisExpr(expr: Expr.This) {
       
    }

    override fun visitSuperExpr(expr: Expr.Super) {
       
    }

    override fun visitCheckExpr(expr: Expr.Check) {
        check(expr.left)
    }

    override fun visitCastExpr(expr: Expr.Cast) {
       check(expr.left)
    }

    override fun visitExprStmt(stmt: Stmt.Expression) {
       check(stmt.expr)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
       
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        stmt.initializer?.let {
            check(it)
            checkType(stmt.type, it.getExprType(), stmt.name, false)
        }
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        scopes.push(stmt)
        check(stmt.statements)
        scopes.pop()
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        check(stmt.thenBranch)
        stmt.elseBranch?.let { check(it) }
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        check(stmt.body)
    }

    override fun visitBreakStmt(stmt: Stmt.Break) {
       
    }

    override fun visitContinueStmt(stmt: Stmt.Continue) {
       
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        scopes.push(stmt)
        check(stmt.body)
        scopes.pop()
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        stmt.value?.let {
            check(it)
            if(!scopes.isEmpty()){
                val enclosingStmt = scopes.peek()
                if(enclosingStmt is Stmt.Function){
                    checkType(enclosingStmt.returnType, it.getExprType(), stmt.keyword, false)
                }
            }
        }

    }

    override fun visitClassStmt(stmt: Stmt.Class) {
        scopes.push(stmt)
        check(stmt.methods)
        scopes.pop()
    }

    override fun visitInterfaceStmt(stmt: Stmt.Interface) {
        scopes.push(stmt)
        check(stmt.methods)
        scopes.pop()
    }

    override fun visitImportStmt(stmt: Stmt.Import) {
       
    }

    override fun visitTryCatchStmt(stmt: Stmt.TryCatch) {
        check(stmt.tryBody)
        check(stmt.catchBody)
    }

    override fun visitThrowStmt(stmt: Stmt.Throw) {
       
    }
}