package sunsetsatellite.interpreter.sunlite

import sunsetsatellite.lang.sunlite.*
import java.util.*

class TypeCollector(val sunlite: Sunlite) : Stmt.Visitor<Unit> {

    val info: MutableList<TypeInfo> = mutableListOf()
    val stack: Stack<Stmt.NamedStmt> = Stack()

    val typeHierarchy: MutableList<TypeHierarchyNode> = mutableListOf()

    var currentFile: String? = null

    data class TypeHierarchyNode(val type: String, val supertypes: List<TypeHierarchyNode>?) {
        override fun toString(): String {
            return "$type ${if (!supertypes.isNullOrEmpty()) "< ${supertypes.joinToString(", ")}" else ""}"
        }
    }

    data class TypeInfo(val parent: Pair<String, Stmt.NamedStmt>?, val self: Pair<String, Stmt.NamedStmt>, val type: Type, val returnType: Type = Type.UNKNOWN, val typeParameters: List<Param>) {
        override fun toString(): String {
            return "${parent?.first}::${self.first} -> $type"
        }
    }
    
    private fun addInfo(parent: Stmt.NamedStmt?, self: Stmt.NamedStmt, type: Type, returnType: Type = Type.UNKNOWN, typeParameters: List<Param>) {
        info.add(TypeInfo(if(parent == null) null else parent.getNameToken().lexeme to parent, self.getNameToken().lexeme to self, type, returnType, typeParameters))
    }

    private fun collect(stmt: Stmt) {
        stmt.accept(this)
    }

    fun collect(statements: List<Stmt>, path: String?) {
        currentFile = path
        for (statement in statements) {
            collect(statement)
        }
    }

    override fun visitExprStmt(stmt: Stmt.Expression) {
        // nothing to collect
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        // nothing to collect
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        val parent = if(stack.isEmpty()) null else stack.peek()
        addInfo(parent, stmt, stmt.type, Type.UNKNOWN, listOf())
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        collect(stmt.statements, currentFile)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        collect(stmt.thenBranch)
        stmt.elseBranch?.let { collect(it) }
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        collect(stmt.body)
    }

    override fun visitBreakStmt(stmt: Stmt.Break) {
        // nothing to collect
    }

    override fun visitContinueStmt(stmt: Stmt.Continue) {
        // nothing to collect
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        val parent = if(stack.isEmpty()) null else stack.peek()

        addInfo(parent, stmt, Type.ofFunction(stmt.name.lexeme, sunlite), stmt.returnType, listOf())

        stmt.params.forEach { addInfo(stmt,
            Stmt.Var(it.token, it.type, null, FieldModifier.NORMAL), it.type,
            Type.UNKNOWN, listOf()) }

        stack.push(stmt)
        collect(stmt.body, currentFile)
        stack.pop()
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        // nothing to collect
    }

    override fun visitClassStmt(stmt: Stmt.Class) {
        val parent = if(stack.isEmpty()) null else stack.peek()

        val type = Type.ofClass(stmt.name.lexeme, sunlite)
        val returnType = Type.ofObject(stmt.name.lexeme, sunlite)

        addInfo(parent, stmt, type, returnType, stmt.typeParameters)

        val supertypes: MutableList<TypeHierarchyNode> = mutableListOf()

        stmt.superclass?.let {
            supertypes.add(TypeHierarchyNode(it.name.lexeme, listOf()))
        }
        stmt.superinterfaces.forEach {
            supertypes.add(TypeHierarchyNode(it.name.lexeme, listOf()))
        }

        typeHierarchy.add(TypeHierarchyNode(stmt.name.lexeme, supertypes))

        stack.push(stmt)
        stmt.methods.forEach { collect(it) }
        stmt.fieldDefaults.forEach { collect(it) }
        stack.pop()
    }

    override fun visitInterfaceStmt(stmt: Stmt.Interface) {
        val parent = if(stack.isEmpty()) null else stack.peek()

        val type = Type.ofClass(stmt.name.lexeme, sunlite)

        addInfo(parent, stmt, type, Type.UNKNOWN, stmt.typeParameters)

        val supertypes: MutableList<TypeHierarchyNode> = mutableListOf()

        stmt.superinterfaces.forEach {
            supertypes.add(TypeHierarchyNode(it.name.lexeme, listOf()))
        }

        typeHierarchy.add(TypeHierarchyNode(stmt.name.lexeme, supertypes))

        stack.push(stmt)
        stmt.methods.forEach { collect(it) }
        stack.pop()
    }

    override fun visitImportStmt(stmt: Stmt.Import) {
        sunlite.imports[stmt.what.literal]?.let { collect(it, stmt.what.literal.toString()) }
    }

}