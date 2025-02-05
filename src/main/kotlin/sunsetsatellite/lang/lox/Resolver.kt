package sunsetsatellite.lang.lox

import java.util.*


class Resolver(private val interpreter: Interpreter, val lox: Lox): Expr.Visitor<Unit>, Stmt.Visitor<Unit> {

    data class ResolutionInfo(var defined: Boolean, var type: Type, var returnType: Type, val paramTypes: List<Type>?)

    private val scopes: Stack<MutableMap<String, ResolutionInfo>> = Stack()
    private val globalScope: MutableMap<String, ResolutionInfo> = mutableMapOf()
    private var currentFunction = FunctionType.NONE
    private var currentFunctionModifier = FunctionModifier.NORMAL
    private var currentClass = ClassType.NONE
    private var currentInterface = ClassType.NONE
    private var inLoop: Int = 0

    var currentFile: String? = null

    override fun visitBinaryExpr(expr: Expr.Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping) {
        resolve(expr.expression)
    }

    override fun visitUnaryExpr(expr: Expr.Unary) {
        resolve(expr.right)
    }

    override fun visitLiteralExpr(expr: Expr.Literal) {
        // nothing to resolve
    }

    override fun visitVariableExpr(expr: Expr.Variable) {
        if (!scopes.isEmpty()) {
            val resolutionInfo = scopes.peek()[expr.name.lexeme]
            if(resolutionInfo != null && !resolutionInfo.defined) {
                lox.error(expr.name,
                    "Can't read local variable in its own initializer.")
                return
            }
        }

        resolveLocal(expr, expr.name)
    }

    override fun visitAssignExpr(expr: Expr.Assign) {
        resolve(expr.value)
        resolveLocal(expr, expr.name)
    }

    override fun visitLogicalExpr(expr: Expr.Logical) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitCallExpr(expr: Expr.Call) {
        resolve(expr.callee)

        for (argument in expr.arguments) {
            resolve(argument)
        }
    }

    override fun visitLambdaExpr(expr: Expr.Lambda) {
        declare(expr.function.name, Type.ofFunction(expr.function.name.lexeme,lox), expr.function.returnType, expr.function.params.map { it.type })
        resolveFunction(expr.function,FunctionType.LAMBDA)
    }

    override fun visitGetExpr(expr: Expr.Get) {
        resolve(expr.obj)
    }

    override fun visitDynamicGetExpr(expr: Expr.DynamicGet) {
        resolve(expr.what)
        resolve(expr.obj)
    }

    override fun visitDynamicSetExpr(expr: Expr.DynamicSet) {
        resolve(expr.what)
        resolve(expr.value)
        resolve(expr.obj)
    }

    override fun visitSetExpr(expr: Expr.Set) {
        resolve(expr.value)
        resolve(expr.obj)
    }

    override fun visitThisExpr(expr: Expr.This) {

        if (currentClass == ClassType.NONE) {
            lox.error(expr.keyword,
                "Unexpected 'this' outside of class.")
            return
        }

        if (currentFunctionModifier == FunctionModifier.STATIC) {
            lox.error(expr.keyword,
                "Unexpected 'this' inside a static method.")
            return
        }

        resolveLocal(expr, expr.keyword)
    }

    override fun visitSuperExpr(expr: Expr.Super) {

        if (currentClass == ClassType.NONE) {
            lox.error(expr.keyword,
                "Unexpected 'super' outside of a class.");
        } else if (currentClass != ClassType.SUBCLASS) {
            lox.error(expr.keyword,
                "Unexpected 'super' in a class with no superclass.");
        }

        resolveLocal(expr, expr.keyword)
    }

    override fun visitCheckExpr(expr: Expr.Check) {
        resolve(expr.left)
    }

    override fun visitCastExpr(expr: Expr.Cast) {
        resolve(expr.left)
    }

    override fun visitExprStmt(stmt: Stmt.Expression) {
        resolve(stmt.expr)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        resolve(stmt.expr)
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        declare(stmt.name, stmt.type)
        stmt.initializer?.let {
            resolve(it)
        }
        define(stmt.name, stmt.type)
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        beginScope()
        resolve(stmt.statements, currentFile)
        endScope()
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        stmt.elseBranch?.let { resolve(it) }
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        inLoop++
        resolve(stmt.condition)
        resolve(stmt.body)
        inLoop--
    }

    override fun visitBreakStmt(stmt: Stmt.Break) {
        if(inLoop <= 0) lox.error(stmt.keyword, "Unexpected `break` outside of loop.")

        // nothing else to resolve
    }

    override fun visitContinueStmt(stmt: Stmt.Continue) {
        if(inLoop <= 0) lox.error(stmt.keyword, "Unexpected `continue` outside of loop.")

        // nothing else to resolve
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        declare(stmt.name, Type.ofFunction(stmt.name.lexeme,lox), stmt.returnType, stmt.params.map { it.type })
        define(stmt.name, Type.ofFunction(stmt.name.lexeme,lox), stmt.returnType, stmt.params.map { it.type })

        resolveFunction(stmt,FunctionType.FUNCTION)
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        if (currentFunction == FunctionType.NONE) {
            lox.error(stmt.keyword, "Unexpected `return` outside of function.")
            return
        }
        stmt.value?.let {
            if (currentFunction == FunctionType.INITIALIZER) {
                lox.error(stmt.keyword,
                    "Can't return a value from an initializer.")
            }

            resolve(it)
        }
    }

    override fun visitClassStmt(stmt: Stmt.Class) {
        val enclosingClass = currentClass
        currentClass = ClassType.CLASS

        declare(stmt.name, Type.ofClass(stmt.name.lexeme,lox))
        define(stmt.name, Type.ofClass(stmt.name.lexeme,lox))

        if (stmt.superclass != null && stmt.name.lexeme == stmt.superclass.name.lexeme) {
            lox.error(stmt.superclass.name,
                "A class can't inherit from itself.");
        }

        if(stmt.superinterfaces.any { return@any stmt.name.lexeme == it.name.lexeme }){
            lox.error(stmt.name,
                "A class can't inherit from itself as an interface.");
        }

        stmt.superclass?.let {
            currentClass = ClassType.SUBCLASS
            resolve(it)
        }

        stmt.superinterfaces.forEach {
            resolve(it)
        }

        if (stmt.superclass != null) {
            beginScope();
            scopes.peek().computeIfAbsent("super") { ResolutionInfo(true, Type.ofObject(stmt.superclass.name.lexeme,lox), Type.UNKNOWN, listOf()) }
        }

        beginScope()
        scopes.peek().computeIfAbsent("this") { ResolutionInfo(true, Type.ofObject(stmt.name.lexeme,lox), Type.UNKNOWN, listOf()) }

        stmt.fieldDefaults.forEach { resolve(it) }

        for (method in stmt.methods) {
            var declaration: FunctionType = FunctionType.METHOD
            if (method.name.lexeme == "init") {
                declaration = FunctionType.INITIALIZER
            }
            resolveFunction(method, declaration)
        }

        endScope()

        if (stmt.superclass != null) endScope();

        currentClass = enclosingClass
    }

    override fun visitInterfaceStmt(stmt: Stmt.Interface) {
        val enclosingClass = currentInterface
        currentInterface = ClassType.INTERFACE

        declare(stmt.name, Type.ofClass(stmt.name.lexeme,lox))
        define(stmt.name, Type.ofClass(stmt.name.lexeme,lox))

        if(stmt.superinterfaces.any { return@any stmt.name.lexeme == it.name.lexeme }){
            lox.error(stmt.name,
                "An interface can't inherit from itself.");
        }

        stmt.superinterfaces.forEach {
            currentInterface = ClassType.SUBINTERFACE
            resolve(it)
        }

        beginScope()

        for (method in stmt.methods) {
            val declaration: FunctionType = FunctionType.METHOD
            resolveFunction(method, declaration)
        }

        endScope()

        currentInterface = enclosingClass
    }

    override fun visitImportStmt(stmt: Stmt.Import) {
        if(currentClass != ClassType.NONE || currentFunction != FunctionType.NONE || currentInterface != ClassType.NONE) {
            lox.error(stmt.keyword,
                "This 'load' statement has to be a top-level statement.")
            return
        }

        lox.imports[stmt.what.literal]?.let { resolve(it, stmt.what.literal.toString()) }
    }

    fun resolve(statements: List<Stmt>, path: String?) {
        currentFile = path
        for (statement in statements) {
            resolve(statement)
        }
        if(Lox.debug) println()
    }

    private fun resolve(stmt: Stmt) {
        stmt.accept(this)
    }

    private fun resolve(expr: Expr) {
        expr.accept(this)
    }

    private fun beginScope() {
        scopes.push(mutableMapOf())
    }

    private fun endScope() {
        scopes.pop()
    }

    private fun declare(name: Token, type: Type, returnType: Type = Type.UNKNOWN, paramTypes: List<Type>? = null) {
        if (scopes.isEmpty()) {
            globalScope.computeIfAbsent(name.lexeme) { ResolutionInfo(false, type, returnType, paramTypes) }
            return
        }

        val scope: MutableMap<String, ResolutionInfo> = scopes.peek()

        if (scope.containsKey(name.lexeme)) {
            lox.error(name,
                "A variable with this name already exists in this scope.")
            return
        }

        scope.computeIfAbsent(name.lexeme) { ResolutionInfo(false, type, returnType, paramTypes) }

    }

    private fun define(name: Token, type: Type, returnType: Type = Type.UNKNOWN, paramTypes: List<Type>? = null) {
        if(Lox.debug) println("variable '${name.lexeme}' is of type '${type.getName()}'")
        if (scopes.isEmpty()) {
            globalScope.computeIfPresent(name.lexeme) { _, resolutionInfo -> resolutionInfo.defined = true; resolutionInfo }
            globalScope.computeIfAbsent(name.lexeme) { ResolutionInfo(true, type, returnType, paramTypes) }
            return
        }
        scopes.peek().computeIfPresent(name.lexeme) { _, resolutionInfo -> resolutionInfo.defined = true; resolutionInfo }
        scopes.peek().computeIfAbsent(name.lexeme) { ResolutionInfo(true, type, returnType, paramTypes) }

    }

    private fun resolveLocal(expr: Expr, name: Token) {
        for (i in scopes.indices.reversed()) {
            if (scopes[i].containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size - 1 - i)
                return
            }
        }
    }

    private fun resolveFunction(function: Stmt.Function, type: FunctionType) {

        if(currentClass == ClassType.NONE && function.modifier == FunctionModifier.STATIC){
            lox.error(function.name,
                "Unexpected 'static' method modifier outside of class.")
            return
        }

        val enclosingFunction = currentFunction
        val enclosingFunctionModifier = currentFunctionModifier

        currentFunction = type
        currentFunctionModifier = function.modifier

        beginScope()
        for (param in function.params) {
            declare(param.token, param.type)
            define(param.token, param.type)
        }
        resolve(function.body, currentFile)
        endScope()

        currentFunction = enclosingFunction
        currentFunctionModifier = enclosingFunctionModifier
    }

}