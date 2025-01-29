package sunsetsatellite.lang.lox

import java.util.*


class Resolver(private val interpreter: Interpreter): Expr.Visitor<Unit>, Stmt.Visitor<Unit> {

    private val scopes: Stack<MutableMap<String, Boolean>> = Stack()
    private var currentFunction = FunctionType.NONE
    private var currentFunctionModifier = FunctionModifier.NONE
    private var currentClass = ClassType.NONE
    private var inLoop: Int = 0

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
        if (!scopes.isEmpty() && scopes.peek()[expr.name.lexeme] == false) {
            Lox.error(expr.name,
                "Can't read local variable in its own initializer.")
            return
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
        resolveFunction(expr.function,FunctionType.LAMBDA)
    }

    override fun visitGetExpr(expr: Expr.Get) {
        resolve(expr.obj)
    }

    override fun visitSetExpr(expr: Expr.Set) {
        resolve(expr.value)
        resolve(expr.obj)
    }

    override fun visitThisExpr(expr: Expr.This) {

        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword,
                "Unexpected 'this' outside of class.")
            return
        }

        if (currentFunctionModifier == FunctionModifier.STATIC) {
            Lox.error(expr.keyword,
                "Unexpected 'this' inside a static method.")
            return
        }

        resolveLocal(expr, expr.keyword)
    }

    override fun visitSuperExpr(expr: Expr.Super) {

        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword,
                "Unexpected 'super' outside of a class.");
        } else if (currentClass != ClassType.SUBCLASS) {
            Lox.error(expr.keyword,
                "Unexpected 'super' in a class with no superclass.");
        }

        resolveLocal(expr, expr.keyword)
    }

    override fun visitExprStmt(stmt: Stmt.Expression) {
        resolve(stmt.expr)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        resolve(stmt.expr)
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        declare(stmt.name)
        stmt.initializer?.let { resolve(it) }
        define(stmt.name)
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        beginScope()
        resolve(stmt.statements)
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
        if(inLoop <= 0) Lox.error(stmt.keyword, "Unexpected `break` outside of loop.")

        // nothing else to resolve
    }

    override fun visitContinueStmt(stmt: Stmt.Continue) {
        if(inLoop <= 0) Lox.error(stmt.keyword, "Unexpected `continue` outside of loop.")

        // nothing else to resolve
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        declare(stmt.name)
        define(stmt.name)

        resolveFunction(stmt,FunctionType.FUNCTION)
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Unexpected `return` outside of function.")
            return
        }
        stmt.value?.let {

            if (currentFunction == FunctionType.INITIALIZER) {
                Lox.error(stmt.keyword,
                    "Can't return a value from an initializer.")
            }

            resolve(it)
        }
    }

    override fun visitClassStmt(stmt: Stmt.Class) {
        val enclosingClass = currentClass
        currentClass = ClassType.CLASS

        declare(stmt.name)
        define(stmt.name)

        if (stmt.superclass != null && stmt.name.lexeme == stmt.superclass.name.lexeme) {
            Lox.error(stmt.superclass.name,
                "A class can't inherit from itself.");
        }

        stmt.superclass?.let {
            currentClass = ClassType.SUBCLASS
            resolve(it)
        }

        if (stmt.superclass != null) {
            beginScope();
            scopes.peek()["super"] = true;
        }

        beginScope()
        scopes.peek()["this"] = true

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

    fun resolve(statements: List<Stmt>) {
        for (statement in statements) {
            resolve(statement)
        }
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

    private fun declare(name: Token) {
        if (scopes.isEmpty()) return

        val scope: MutableMap<String, Boolean> = scopes.peek()

        if (scope.containsKey(name.lexeme)) {
            Lox.error(name,
                "A variable with this name already exists in this scope.")
            return
        }

        scope[name.lexeme] = false
    }

    private fun define(name: Token) {
        if (scopes.isEmpty()) return
        scopes.peek()[name.lexeme] = true
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
            Lox.error(function.name,
                "Unexpected 'static' method modifier outside of class.")
            return
        }

        val enclosingFunction = currentFunction
        val enclosingFunctionModifier = currentFunctionModifier

        currentFunction = type
        currentFunctionModifier = function.modifier

        beginScope()
        for (param in function.params) {
            declare(param)
            define(param)
        }
        resolve(function.body)
        endScope()

        currentFunction = enclosingFunction
        currentFunctionModifier = enclosingFunctionModifier
    }

}