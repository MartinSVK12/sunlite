package sunsetsatellite.lang.sunlite

import sunsetsatellite.vm.sunlite.SLNativeFuncObj
import sunsetsatellite.vm.sunlite.VM

class TypeCollector(val sunlite: Sunlite): Stmt.Visitor<Unit> {

    var currentClass: Stmt.Class? = null

    abstract inner class ElementPrototype {
       abstract fun getElementType(): Type
    }

    inner class VariablePrototype(val type: Type): ElementPrototype() {
        override fun toString(): String {
            return ": $type"
        }

        override fun getElementType(): Type {
            return type
        }
    }

    inner class FunctionPrototype(
        val name: Token,
        val params: List<Param>,
        val returnType: Type,
        val typeParams: List<Type> = listOf()
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
    }

    class Scope(val name: Token, val contents: MutableMap<Token, ElementPrototype>, val depth: Int = -1, var outer: Scope? = null, var inner: MutableList<Scope> = mutableListOf()) {
        override fun toString(): String {
            return "scope '${name.lexeme}'"
        }
    }

    val typeHierarchy: MutableMap<String, String> = mutableMapOf()
    val typeScopes: MutableList<Scope> = mutableListOf()
    var path: String? = null
    var currentScope: Scope? = Scope(Token.identifier("<global>",-1,"<global>"), mutableMapOf())

    init {
        typeScopes.add(currentScope!!)
        VM.globals.filter { it.value is SLNativeFuncObj }.forEach {
            addFunction(Token.identifier(it.key,-1,"<global>"),listOf(), (it.value as SLNativeFuncObj).value.returnType)
        }
    }

    fun addVariable(name: Token, type: Type) {
        currentScope?.contents?.put(name, VariablePrototype(type))
    }
    fun addFunction(name: Token, params: List<Param>, returnType: Type, typeParams: List<Type> = listOf()) {
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
        val globalScope = typeScopes[0]
        if(globalScope.contents.keys.map { it.lexeme }.contains(name.lexeme)){
            return globalScope.contents.mapKeys { it.key.lexeme }[name.lexeme]
        }
        val scope = getValidScope(typeScopes.firstOrNull(), name, enclosing, offset)
        if(scope == null) return null
        return scope.contents.mapKeys { it.key.lexeme }[name.lexeme]
    }

    override fun visitExprStmt(stmt: Stmt.Expression) {
        // nothing to collect
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        // nothing to collect
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        addVariable(stmt.name, stmt.type)
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        addScope(Token.identifier("<block>",stmt.lineNumber,stmt.currentFile))
        stmt.statements.forEach { it.accept(this) }
        removeScope()
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        stmt.thenBranch.accept(this)
        stmt.elseBranch?.accept(this)
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        stmt.body.accept(this)
    }

    override fun visitBreakStmt(stmt: Stmt.Break) {
        // nothing to collect
    }

    override fun visitContinueStmt(stmt: Stmt.Continue) {
        // nothing to collect
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        val typeParams = stmt.typeParams.map { it.type }.toMutableList()
        currentClass?.typeParameters?.map { it.type }?.forEach { typeParams.add(it) }
        addFunction(stmt.name, stmt.params, stmt.returnType, typeParams)
        addScope(stmt.name)
        stmt.params.forEach { addVariable(it.token, it.type) }
        stmt.body.forEach { it.accept(this) }
        removeScope()
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        // nothing to collect
    }

    override fun visitClassStmt(stmt: Stmt.Class) {
        val classParams = stmt.methods.find { it.name.lexeme == "init" }?.params ?: listOf()
        addVariable(stmt.name, Type.ofClass(stmt.name.lexeme, classParams))
        addScope(stmt.name)
        currentClass = stmt
        typeHierarchy[stmt.name.lexeme] = stmt.superclass?.name?.lexeme ?: "<nil>"
        stmt.superclass?.let {
            addVariable(Token.identifier("<superclass>",it.getLine(),it.getFile()),Type.ofClass(it.name.lexeme))
        }
        stmt.fieldDefaults.forEach { it.accept(this) }
        stmt.methods.forEach { it.accept(this) }
        stmt.typeParameters.forEach { addVariable(Token.identifier("<${it.token.lexeme}>",it.token.line, it.token.file), Type.Parameter(it.token)) }
        currentClass = null
        removeScope()
    }

    override fun visitInterfaceStmt(stmt: Stmt.Interface) {
        addVariable(stmt.name, Type.ofClass(stmt.name.lexeme))
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
        // nothing to collect
    }

}