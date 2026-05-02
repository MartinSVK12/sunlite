package sunsetsatellite.sunlite.lang

import sunsetsatellite.sunlite.vm.SLString
import sunsetsatellite.sunlite.vm.VM
import java.util.*

class TypeChecker(val sunlite: Sunlite, val vm: VM?) : Expr.Visitor<Unit>, Stmt.Visitor<Unit> {

    val scopes: Stack<Stmt.NamedStmt> = Stack()
    var path: Token? = null

    fun check(statements: List<Stmt>, path: String? = null) {
        path?.let { this.path = Token.identifier(it, -1, it) }
        statements.forEach { it.accept(this) }
    }

    fun check(stmt: Stmt) {
        stmt.accept(this)
    }

    fun check(expr: Expr) {
        expr.accept(this)
    }

    fun checkType(
        expected: Type,
        actual: Type,
        runtime: Boolean = false,
        token: Token? = null
    ) {
        if (Sunlite.debug) {
            //sunlite.printInfo("Checking if type '$expected' matches '$actual' at '${token?.lexeme ?: "<runtime>"}'")
        }
        val valid = Type.contains(actual, expected, sunlite)
        if(expected is Type.Parameter && actual != Type.UNKNOWN && runtime) return
        if (!valid) {
            if (runtime && vm != null) {
                vm.throwException(0, vm.makeExceptionObject("TypeError: Expected '$expected' but got '$actual'."))
            } else {
                sunlite.error(token!!, "Expected '$expected' but got '$actual'.")
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
        checkType(expr.value.getExprType(), expr.getExprType(), false, expr.name)
    }

    override fun visitLogicalExpr(expr: Expr.Logical) {
        check(expr.left)
        check(expr.right)
    }

    override fun visitCallExpr(expr: Expr.Call) {
        check(expr.callee)
        expr.arguments.forEach { check(it) }

        val calleeType = expr.callee.getExprType()
        if (calleeType is Type.Reference) {
            if (calleeType.type == PrimitiveType.FUNCTION || calleeType.type == PrimitiveType.CLASS) {
                val typeArgs = expr.typeArgs.toMutableList()
                /*scopes.forEach {
                    if(it is Stmt.GenericStmt){
                        typeArgs.addAll(it.getTypeParams())
                    }
                }*/
                val args = expr.arguments
                var params = calleeType.params
                params = params.filter { it.type.getDescriptor().contains("G") }.map { param ->
                    if(param.type is Type.Reference){
                        val o = object {
                            val primitive = param.type.type
                            val ref = param.type.ref
                            var params = param.type.params
                            var returnType = param.type.returnType
                            val typeParams = mutableListOf(*param.type.typeParams.toTypedArray())
                        }
                        o.params = o.params.map { refParam ->
	                        if(refParam.type is Type.Parameter){
                                typeArgs.find { it.token.lexeme == refParam.type.name.lexeme }?.let {
                                    return@map Param(refParam.token, it.type)
                                }
                            }
	                        return@map refParam
                        }
                        o.returnType = o.returnType.let { type ->
                            if(type is Type.Parameter){
                                val reifiedType = typeArgs.find { it.token.lexeme == type.name.lexeme }?.let {
                                    return@let it.type
                                }
                                return@let reifiedType ?: type
                            }
                            return@let type
                        }
                        return@map Param(param.token, Type.Reference(o.primitive, o.ref, o.returnType, o.params, o.typeParams))
                    }
                    return@map param
                }
                var i = 0
                for (it in args.zip(params)) {
                    if (it.second.type is Type.Parameter) {
                        if (typeArgs.isEmpty()) {
                            //sunlite.warn(expr.paren, "Cannot determine concrete type of ${it.second.type}")
                            //sunlite.error(expr.paren, "Missing ${it.second.type}")
                            continue
                        }
                        checkType(typeArgs[i].type, it.first.getExprType(), false, expr.paren)
                        i++
                    } else {
                        checkType(it.second.type, it.first.getExprType(), false, expr.paren)
                    }
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
        check(expr.obj)
        check(expr.what)
        var indexType: Type = Type.INT
        if (expr.obj.getExprType() is Type.Reference) {
            val ref = expr.obj.getExprType() as Type.Reference
            if (ref.type == PrimitiveType.TABLE) {
                indexType = ref.typeParams[0].type
            }
        }
        if(expr.obj.getExprType() == Type.UNKNOWN){
            indexType = Type.UNKNOWN
        }
        checkType(indexType, expr.what.getExprType(), false, expr.token)
    }

    override fun visitArraySetExpr(expr: Expr.ArraySet) {
        check(expr.obj)
        check(expr.what)
        check(expr.value)
        val exprType = expr.getExprType()
        var indexType: Type = Type.INT
        if (expr.obj.getExprType() is Type.Reference) {
            val ref = expr.obj.getExprType() as Type.Reference
            if (ref.type == PrimitiveType.TABLE) {
                indexType = ref.typeParams[0].type
            }
        }
        checkType(indexType, expr.what.getExprType(), false, expr.token)
        checkType(exprType, expr.value.getExprType(), false, expr.token)
    }

    override fun visitSetExpr(expr: Expr.Set) {
        check(expr.obj)
        check(expr.value)
        var exprType = expr.getExprType()
        if (exprType is Type.Parameter && expr.obj.getExprType() is Type.Reference && (expr.obj.getExprType() as Type.Reference).type == PrimitiveType.OBJECT) {
            val receiverType = expr.obj.getExprType() as Type.Reference
            receiverType.typeParams.find { it.token.lexeme == exprType.name.lexeme }?.let {
                exprType = it.type
            }
        }
        checkType(exprType, expr.value.getExprType(), false, expr.name)
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

    override fun visitArrayExpr(expr: Expr.Array) {
        expr.expr.forEach { check(it) }
    }

    override fun visitExprStmt(stmt: Stmt.Expression) {
        check(stmt.expr)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {

    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        stmt.initializer?.let {
            check(it)
            checkType(stmt.type, it.getExprType(), false, stmt.name)
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
            if (!scopes.isEmpty()) {
                val enclosingStmt = scopes.peek()
                if (enclosingStmt is Stmt.Function) {
                    checkType(enclosingStmt.returnType, it.getExprType(), false, stmt.keyword)
                }
            }
        }

    }

    override fun visitClassStmt(stmt: Stmt.Class) {
        scopes.push(stmt)
        check(stmt.methods)
        check(stmt.fieldDefaults)
        scopes.pop()
    }

    override fun visitInterfaceStmt(stmt: Stmt.Interface) {
        scopes.push(stmt)
        check(stmt.methods)
        scopes.pop()
    }

    override fun visitIncludeStmt(stmt: Stmt.Include) {

    }

    override fun visitImportStmt(stmt: Stmt.Import) {

    }

    override fun visitPackageStmt(stmt: Stmt.Package) {

    }

    override fun visitTryCatchStmt(stmt: Stmt.TryCatch) {
        check(stmt.tryBody)
        check(stmt.catchBody)
    }

    override fun visitThrowStmt(stmt: Stmt.Throw) {
        checkType(Type.ofObject("Exception"), stmt.expr.getExprType(), token = stmt.keyword)
    }

    override fun visitAnnotationStmt(stmt: Stmt.Annotation) {

    }
}