package sunsetsatellite.lang.lox

import java.util.*

class TypeChecker(val collector: TypeCollector, val lox: Lox) : Stmt.Visitor<Unit>, Expr.Visitor<Unit> {

    val resolver: TypeResolver = TypeResolver(collector, lox)
    val stack: Stack<Stmt.NamedStmt> = Stack()

    var currentFile: String? = null

    private fun check(expr: Expr) {
        try {
            expr.accept(this)
        } catch (_: NoSuchElementException) { }

    }

    private fun check(stmt: Stmt) {
        try {
            stmt.accept(this)
        } catch (_: NoSuchElementException) { }
    }

    fun check(statements: List<Stmt>, path: String?) {
        currentFile = path
        for (statement in statements) {
            check(statement)
        }
    }

    override fun visitExprStmt(stmt: Stmt.Expression) {
        check(stmt.expr)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        check(stmt.expr)
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        stmt.initializer?.let {
            val initType = resolver.resolve(it, if(stack.isEmpty()) null else stack.peek())
            checkType(stmt.type, initType, stmt.name)
        }
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        check(stmt.statements, currentFile)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        check(stmt.condition)
        check(stmt.thenBranch)
        stmt.elseBranch?.let { check(it) }
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        check(stmt.condition)
        check(stmt.body)
    }

    override fun visitBreakStmt(stmt: Stmt.Break) {

    }

    override fun visitContinueStmt(stmt: Stmt.Continue) {

    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        stack.push(stmt)
        check(stmt.body, currentFile)
        stack.pop()
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        val parent = if(stack.empty()) null else stack.peek()
        stmt.value?.let { check(it) }
        val funcReturnType: Type = collector.info.first { it.self == parent }.returnType
        val returnType = stmt.value?.let { resolver.resolve(it, parent) } ?: Type.UNKNOWN
        checkType(funcReturnType,returnType,stmt.keyword)
    }

    override fun visitClassStmt(stmt: Stmt.Class) {
        stack.push(stmt)
        check(stmt.methods,currentFile)
        check(stmt.fieldDefaults,currentFile)
        stack.pop()
    }

    override fun visitInterfaceStmt(stmt: Stmt.Interface) {
        stack.push(stmt)
        check(stmt.methods,currentFile)
        stack.pop()
    }

    override fun visitImportStmt(stmt: Stmt.Import) {
        lox.imports[stmt.what.literal]?.let { check(it, stmt.what.literal.toString()) }
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
        val varType: Type = collector.info.first { it.self.first == expr.name.lexeme }.type
        val assignType = resolver.resolve(expr, if(stack.isEmpty()) null else stack.peek())
        checkType(varType, assignType, expr.name)
    }

    override fun visitLogicalExpr(expr: Expr.Logical) {
        check(expr.left)
        check(expr.right)
    }

    override fun visitCallExpr(expr: Expr.Call) {
        check(expr.callee)
        expr.arguments.forEach { check(it) }

        val parent = if(stack.empty()) null else stack.peek()

        if(expr.callee is Expr.NamedExpr){

            val name = expr.callee.getNameToken().lexeme

            val calleeType = resolver.resolve(expr.callee,parent,true)

            val info: TypeCollector.TypeInfo?
            if(parent != null){
                info = getTypeInfo(parent.getNameToken().lexeme, name)
            } else {
                info = collector.info.first { it.self.first == name }
            }

            if(info.self.second is Stmt.Function){
                val params = (info.self.second as Stmt.Function).params

                if(expr.arguments.size != params.size) {
                    lox.error(expr.paren, "Expected ${params.size} arguments but got ${expr.arguments.size}.")
                    return
                }

                params.forEachIndexed { index, it ->
                    val arg = expr.arguments[index]
                    val argType = resolver.resolve(arg, parent)
                    var actualType = it.type
                    if(actualType is Type.Parameter && calleeType is Type.Reference){
                        val calleeInfo = collector.info.filter { it.parent == parent }.first { it.self.first == calleeType.ref }
                        val typeParameters = calleeInfo.typeParameters
                        var typeParamIndex = -1
                        typeParameters.forEachIndexed { i, typeParam -> if(typeParam.token.lexeme == (actualType as Type.Parameter).name.lexeme) typeParamIndex = i }
                        if(typeParamIndex > -1 && calleeType.typeParameters.size > typeParamIndex){
                            actualType = calleeType.typeParameters[typeParamIndex]
                        } else {
                            lox.error(expr.paren,"Unspecified type parameter $actualType.")
                        }
                    }
                    checkType(actualType,argType,expr.paren,false,it.token.lexeme)
                }
            }
        }
    }

    private fun getTypeInfo(
        parent: String?,
        name: String
    ): TypeCollector.TypeInfo {
        var info = collector.info.filter { parent == it.parent?.first }
            .firstOrNull { it.self.first == name }
        if (info == null) {
            info = collector.info.filter { it.parent == null }.first { it.self.first == name }
        }
        return info
    }

    override fun visitLambdaExpr(expr: Expr.Lambda) {
        visitFunctionStmt(expr.function)
    }

    override fun visitGetExpr(expr: Expr.Get) {
        check(expr.obj)
    }

    override fun visitDynamicGetExpr(expr: Expr.DynamicGet) {
        // intentionally unchecked
    }

    override fun visitDynamicSetExpr(expr: Expr.DynamicSet) {
        // intentionally unchecked
    }

    override fun visitSetExpr(expr: Expr.Set) {
        check(expr.value)
        check(expr.obj)
        val parent = if(stack.empty()) null else stack.peek()
        val setType = resolver.resolve(expr.value, parent)

        var parentName: String? = null
        if(expr.obj is Expr.NamedExpr){
            parentName = expr.obj.getNameToken().lexeme
        }

        val info: TypeCollector.TypeInfo = getTypeInfo(parentName,expr.name.lexeme)

        val objType = info.type
        checkType(objType, setType, expr.name)
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

    fun checkType(
        expected: Type,
        actual: Type,
        token: Token,
        runtime: Boolean = false,
        expectedName: String? = null,
    ) {
        if(Lox.debug) println("checking if '${expectedName ?: token.lexeme}' of type '${expected.getName()}' matches type '${actual.getName()}'")
        val valid = Type.contains(actual, expected)

        if(expected is Type.Parameter || actual is Type.Parameter && !valid){
            lox.warn(token, "Unable to clarify concrete type of generic type, using '$actual' as '${expected}'.")
            return
        }

        if(actual == Type.UNKNOWN){
            if(runtime) {
                throw LoxRuntimeError(token,"Casting failed, couldn't determine if ${if (expectedName != null) "'${expectedName}' of type is " else "type is "}'${expected.getName()}', got '${actual.getName()}'.")
            }
            lox.warn(token,"Unchecked cast, couldn't determine if ${if (expectedName != null) "'${expectedName}' of type is " else "type is "}'${expected.getName()}', got '${actual.getName()}'.")
            return
        }

        if(!valid){
            if(!runtime){
                lox.error(token, "Type mismatch, expected ${if(expectedName != null) "'${expectedName}' to be " else ""}'${expected.getName()}' but got '${actual.getName()}'.")
            } else {
                throw LoxRuntimeError(token, "Type mismatch, expected ${if(expectedName != null) "'${expectedName}' to be " else ""}'${expected.getName()}' but got '${actual.getName()}'.")
            }
            return
        }
        if(Lox.debug) println("type check succeeded")
    }
}