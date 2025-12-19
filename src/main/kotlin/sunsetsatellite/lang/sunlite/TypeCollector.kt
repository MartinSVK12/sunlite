package sunsetsatellite.lang.sunlite

import sunsetsatellite.vm.sunlite.SLNativeFuncObj
import sunsetsatellite.vm.sunlite.VM

class TypeCollector(val sunlite: Sunlite): Stmt.Visitor<Unit>, Expr.Visitor<Unit> {

    var currentClass: Stmt.Class? = null
    var currentScopeCandidate: Scope? = null

    abstract inner class ElementPrototype {
        abstract fun getElementType(): Type
        abstract fun isConstant(): Boolean
    }

    inner class VariablePrototype(val type: Type, val constant: Boolean): ElementPrototype() {
        override fun toString(): String {
            return ": $type"
        }

        override fun getElementType(): Type {
            return type
        }

        override fun isConstant(): Boolean {
            return constant
        }
    }

    inner class FunctionPrototype(
        val name: Token,
        val params: List<Param>,
        val returnType: Type,
        val typeParams: List<Param> = listOf()
    ): ElementPrototype(){
        override fun toString(): String {
            return "(${params.joinToString()}): $returnType"
        }

        override fun getElementType(): Type {
            if(typeParams.isNotEmpty()){
                return Type.ofGenericFunction(name.lexeme, returnType, params, typeParams)
            }
            return Type.ofFunction(name.lexeme, returnType, params)
        }

        override fun isConstant(): Boolean {
            return true
        }
    }

    class Scope(val name: Token, val contents: MutableMap<Token, ElementPrototype>, val depth: Int = -1, var outer: Scope? = null, var inner: MutableList<Scope> = mutableListOf()) {
        override fun toString(): String {
            return "scope '${name.lexeme}'"
        }
    }

    val typeHierarchy: MutableMap<String, Triple<String,List<String>,List<String>>> = mutableMapOf()
    val typeScopes: MutableList<Scope> = mutableListOf()
    var path: String? = null
    var currentScope: Scope? = Scope(Token.identifier("<global>",-1,"<global>"), mutableMapOf())

    init {
        typeScopes.add(currentScope!!)
        VM.globals.filter { it.value is SLNativeFuncObj }.forEach {
            addFunction(Token.identifier(it.key,-1,"<global>"),listOf(), (it.value as SLNativeFuncObj).value.returnType)
        }
    }

    fun addVariable(name: Token, type: Type, constant: Boolean = false) {
        if(currentScope?.contents?.mapKeys { it.key.lexeme }?.containsKey(name.lexeme) == true) sunlite.error(name, "Variable '${name.lexeme}' already declared in this scope.")
        currentScope?.contents?.put(name, VariablePrototype(type, constant))
    }
    fun addFunction(name: Token, params: List<Param>, returnType: Type, typeParams: List<Param> = listOf()) {
        if(typeScopes[0].contents.mapKeys { it.key.lexeme }.containsKey(name.lexeme)) sunlite.error(name, "Cannot overwrite global function '${name.lexeme}'.")
        if(currentScope?.contents?.mapKeys { it.key.lexeme }?.containsKey(name.lexeme) == true) sunlite.error(name, "Function '${name.lexeme}' already declared in this scope.")
        currentScope?.contents?.put(name, FunctionPrototype(name, params, returnType, typeParams))
    }

    fun addScope(name: Token){
        if(currentScope == null){
            currentScope = Scope(name, mutableMapOf())
            typeScopes.add(currentScope!!)
        } else {
            val scope = Scope(name, mutableMapOf(), currentScope!!.depth+1)
            currentScope!!.inner.add(scope)
            scope.outer = currentScope
            currentScope = scope
        }
    }

    fun removeScope(){
        currentScope = currentScope?.outer
    }

    fun collect(statements: List<Stmt>,path: String? = null){
        this.path = path
        addScope(Token.identifier(path.toString(),-1,path))
        statements.forEach { it.accept(this) }
        removeScope()
    }

    fun getValidScope(scope: Scope?, name: Token, enclosing: Token? = null, offset: Int = 0, enclosingScope: Scope? = null): Scope? {
        var enclosingS: Scope? = enclosingScope
        if(scope == null) return null
        if(scope.name.lexeme == enclosing?.lexeme){
            enclosingS = scope
        }
        if(scope.contents.keys.map { it.lexeme }.contains(name.lexeme)){
            currentScopeCandidate = scope
        }
        if(enclosingS != null && scope.depth <= enclosingS.depth + offset && scope.contents.keys.map { it.lexeme }.contains(name.lexeme)){
            return scope
        } else {
            var valid: Scope? = null
            scope.inner.forEach {
                valid = getValidScope(it, name, enclosing, offset, enclosingS)
                if(valid != null) return valid
            }
            return valid
        }
    }

    fun findType(name: Token, enclosing: Token? = null, offset: Int = 0): ElementPrototype? {
        currentScopeCandidate = null
        val globalScope = typeScopes[0]
        if(globalScope.contents.keys.map { it.lexeme }.contains(name.lexeme)){
            return globalScope.contents.mapKeys { it.key.lexeme }[name.lexeme]
        }
        val scope = getValidScope(typeScopes.firstOrNull(), name, enclosing, offset)
        if(scope == null && currentScopeCandidate == null) return null
        if(currentScopeCandidate != null) return currentScopeCandidate!!.contents.mapKeys { it.key.lexeme }[name.lexeme]
        return scope?.contents?.mapKeys { it.key.lexeme }[name.lexeme]
    }

    override fun visitExprStmt(stmt: Stmt.Expression) {
        stmt.expr.accept(this)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        // nothing to collect
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        addVariable(stmt.name, stmt.type, stmt.modifier == FieldModifier.CONST || stmt.modifier == FieldModifier.STATIC_CONST)
        stmt.initializer?.accept(this)
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        addScope(Token.identifier("<block>",stmt.lineNumber,stmt.currentFile))
        stmt.statements.forEach { it.accept(this) }
        removeScope()
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        stmt.condition.accept(this)
        stmt.thenBranch.accept(this)
        stmt.elseBranch?.accept(this)
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        stmt.condition.accept(this)
        stmt.body.accept(this)
    }

    override fun visitBreakStmt(stmt: Stmt.Break) {
        // nothing to collect
    }

    override fun visitContinueStmt(stmt: Stmt.Continue) {
        // nothing to collect
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        val typeParams = stmt.typeParams.toMutableList()
        currentClass?.typeParameters?.forEach { typeParams.add(it) }
        addFunction(stmt.name, stmt.params, stmt.returnType, typeParams)
        addScope(stmt.name)
        stmt.params.forEach { addVariable(it.token, it.type) }
        stmt.body.forEach { it.accept(this) }
        removeScope()
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        stmt.value?.accept(this)
    }

    override fun visitClassStmt(stmt: Stmt.Class) {
        val classParams = stmt.methods.find { it.name.lexeme == "init" }?.params ?: listOf()
        addVariable(stmt.name, Type.ofClass(stmt.name.lexeme, classParams))
        addScope(stmt.name)
        currentClass = stmt
        val superclass = stmt.superclass?.name?.lexeme ?: "<nil>"
        val superinterfaces = stmt.superinterfaces.map { it.name.lexeme }.toMutableList()
        if(typeHierarchy.containsKey(stmt.name.lexeme)) sunlite.error(stmt.name, "Class '${stmt.name.lexeme}' already defined.")
        typeHierarchy[stmt.name.lexeme] = Triple(superclass, superinterfaces, stmt.typeParameters.map { it.token.lexeme })
        stmt.superclass?.let {
            addVariable(Token.identifier("<superclass>",it.getLine(),it.getFile()),Type.ofClass(it.name.lexeme))
        }
        stmt.superinterfaces.forEach {
            addVariable(Token.identifier("<superinterface ${it.name.lexeme}>",it.getLine(),it.getFile()),Type.ofClass(it.name.lexeme))
        }
        stmt.fieldDefaults.forEach { it.accept(this) }
        stmt.methods.forEach { it.accept(this) }
        stmt.typeParameters.forEach { addVariable(Token.identifier("<${it.token.lexeme}>",it.token.line, it.token.file), Type.Parameter(it.token)) }
        currentClass = null
        removeScope()
    }

    override fun visitInterfaceStmt(stmt: Stmt.Interface) {
        addVariable(stmt.name, Type.ofClass(stmt.name.lexeme))
        val superinterfaces = stmt.superinterfaces.map { it.name.lexeme }.toMutableList()
        if(typeHierarchy.containsKey(stmt.name.lexeme)) sunlite.error(stmt.name, "Class '${stmt.name.lexeme}' already defined.")
        typeHierarchy[stmt.name.lexeme] = Triple("<nil>",superinterfaces,stmt.typeParameters.map { it.token.lexeme })
        addScope(stmt.name)
        stmt.methods.forEach { it.accept(this) }
        removeScope()
    }

    override fun visitImportStmt(stmt: Stmt.Import) {
        // nothing to collect
    }

    override fun visitTryCatchStmt(stmt: Stmt.TryCatch) {
        stmt.tryBody.accept(this)
        stmt.catchBody.accept(this)
    }

    override fun visitThrowStmt(stmt: Stmt.Throw) {
        stmt.expr.accept(this)
    }

    override fun visitBinaryExpr(expr: Expr.Binary) {
        expr.left.accept(this)
        expr.right.accept(this)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping) {
        expr.expression.accept(this)
    }

    override fun visitUnaryExpr(expr: Expr.Unary) {
        expr.right.accept(this)
    }

    override fun visitLiteralExpr(expr: Expr.Literal) {

    }

    override fun visitVariableExpr(expr: Expr.Variable) {

    }

    override fun visitAssignExpr(expr: Expr.Assign) {
        expr.value.accept(this)
    }

    override fun visitLogicalExpr(expr: Expr.Logical) {
        expr.left.accept(this)
        expr.right.accept(this)
    }

    override fun visitCallExpr(expr: Expr.Call) {
        expr.callee.accept(this)
        expr.arguments.forEach { it.accept(this) }
    }

    override fun visitLambdaExpr(expr: Expr.Lambda) {
        expr.function.accept(this);
    }

    override fun visitGetExpr(expr: Expr.Get) {
        expr.obj.accept(this)
    }

    override fun visitArrayGetExpr(expr: Expr.ArrayGet) {
        expr.obj.accept(this)
        expr.what.accept(this)
    }

    override fun visitArraySetExpr(expr: Expr.ArraySet) {
        expr.obj.accept(this)
        expr.what.accept(this)
        expr.value.accept(this)
    }

    override fun visitSetExpr(expr: Expr.Set) {
        expr.obj.accept(this)
        expr.value.accept(this)
    }

    override fun visitThisExpr(expr: Expr.This) {

    }

    override fun visitSuperExpr(expr: Expr.Super) {

    }

    override fun visitCheckExpr(expr: Expr.Check) {
        expr.left.accept(this)
    }

    override fun visitCastExpr(expr: Expr.Cast) {
        expr.left.accept(this)
    }

}