package sunsetsatellite.sunlite.lang

abstract class Stmt : Element {

    data class Expression(val expr: Expr) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitExprStmt(this)
        }

        override fun getLine(): Int {
            return expr.getLine()
        }

        override fun getFile(): String? {
            return expr.getFile()
        }
    }

    data class Print(val expr: Expr) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitPrintStmt(this)
        }

        override fun getLine(): Int {
            return expr.getLine()
        }

        override fun getFile(): String? {
            return expr.getFile()
        }
    }

    data class Var(val name: Token, val type: Type, val initializer: Expr?, val modifier: FieldModifier) : Stmt(),
        NamedStmt {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitVarStmt(this)
        }

        override fun getLine(): Int {
            return name.line
        }

        override fun getFile(): String? {
            return name.file
        }

        override fun getNameToken(): Token {
            return name
        }
    }

    data class Destruct(val vars: List<Param>, var collection: Expr?, val modifier: FieldModifier) : Stmt(), VirtualStmt {
        override fun <R> accept(visitor: Visitor<R>): R {
            throw IllegalStateException("Stmt.Destruct is a VirtualStmt and cannot be visited.")
        }

        override fun getLine(): Int {
            return vars.first().token.line
        }

        override fun getFile(): String? {
            return vars.first().token.file
        }

        override fun decompose(): List<Stmt> {
           val stmts = mutableListOf<Stmt>()
            vars.forEachIndexed { index, it ->
                val initializer = Expr.ArrayGet(collection!!, Expr.Literal(index, getLine(), getFile(), Type.INT), it.token)
                var type = it.type
                if(type == Type.UNKNOWN){
                    type = initializer.getExprType()
                }
                stmts.add(Var(it.token, type, initializer, modifier))
            }
           return stmts
        }

    }

    data class Block(val statements: List<Stmt>, val lineNumber: Int, val currentFile: String?) : Stmt(), NamedStmt {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitBlockStmt(this)
        }

        override fun getLine(): Int {
            return lineNumber
        }

        override fun getFile(): String? {
            return currentFile
        }

        override fun getNameToken(): Token {
            return Token.identifier("<block>", getLine(), getFile())
        }
    }

    data class If(val condition: Expr, val thenBranch: Stmt, val elseBranch: Stmt?) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitIfStmt(this)
        }

        override fun getLine(): Int {
            return condition.getLine()
        }

        override fun getFile(): String? {
            return condition.getFile()
        }
    }

    data class While(val condition: Expr, val body: Stmt) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitWhileStmt(this)
        }

        override fun getLine(): Int {
            return condition.getLine()
        }

        override fun getFile(): String? {
            return condition.getFile()
        }
    }

    class Break(val keyword: Token) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitBreakStmt(this)
        }

        override fun getFile(): String? {
            return keyword.file
        }

        override fun getLine(): Int {
            return keyword.line
        }

    }

    class Continue(val keyword: Token) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitContinueStmt(this)
        }

        override fun getFile(): String? {
            return keyword.file
        }

        override fun getLine(): Int {
            return keyword.line
        }
    }

    data class Function(
        val name: Token,
        val type: FunctionType,
        val params: List<Param>,
        val body: List<Stmt>,
        var modifier: Array<FunctionModifier>,
        val returnType: Type,
        val typeParameters: List<Param>,
        val annotation: List<Annotation> = listOf(),
        val receiver: Token? = null
    ) : Stmt(), Annotatable, NamedStmt, GenericStmt {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitFunctionStmt(this)
        }

        override fun getLine(): Int {
            return name.line
        }

        override fun getFile(): String? {
            return name.file
        }

        override fun getNameToken(): Token {
            return name
        }

        override fun getAnnotations(): List<Annotation> {
            return annotation
        }

        override fun getTypeParams(): List<Param> {
            return typeParameters
        }
    }

    data class Return(val keyword: Token, val value: Expr?) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitReturnStmt(this)
        }

        override fun getFile(): String? {
            return keyword.file
        }

        override fun getLine(): Int {
            return keyword.line
        }
    }

    data class Include(val keyword: Token, val what: Token) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitIncludeStmt(this)
        }

        override fun getFile(): String? {
            return keyword.file
        }

        override fun getLine(): Int {
            return keyword.line
        }
    }

    data class Import(val keyword: Token, val what: Token, val location: String, val aliases: List<Token> = listOf()) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitImportStmt(this)
        }

        override fun getFile(): String? {
            return keyword.file
        }

        override fun getLine(): Int {
            return keyword.line
        }
    }

    data class Module(val keyword: Token, val path: Token, var stmts: List<Stmt>) : Stmt(), NamedStmt {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitModuleStmt(this)
        }

        override fun getFile(): String? {
            return keyword.file
        }

        override fun getLine(): Int {
            return keyword.line
        }


        override fun getNameToken(): Token {
            return Token.identifier("<module ${path.lexeme}>", keyword)
        }
    }

    data class Class(
        val name: Token,
        val methods: List<Function>,
        val fieldDefaults: List<Var>,
        val superclass: Expr.Variable?,
        val superinterfaces: List<Expr.Variable>,
        val modifier: ClassModifier,
        val typeParameters: List<Param>,
        val staticInit: Block?
    ) : Stmt(), NamedStmt, GenericStmt {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitClassStmt(this)
        }

        override fun getFile(): String? {
            return name.file
        }

        override fun getLine(): Int {
            return name.line
        }

        override fun getNameToken(): Token {
            return name
        }

        override fun getTypeParams(): List<Param> {
            return typeParameters
        }
    }

    data class Interface(
        val name: Token,
        val methods: List<Function>,
        val superinterfaces: List<Expr.Variable>,
        val typeParameters: List<Param>
    ) : Stmt(), NamedStmt, GenericStmt {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitInterfaceStmt(this)
        }

        override fun getFile(): String? {
            return name.file
        }

        override fun getLine(): Int {
            return name.line
        }

        override fun getNameToken(): Token {
            return name
        }

        override fun getTypeParams(): List<Param> {
            return typeParameters
        }
    }

    data class TryCatch(
        val tryToken: Token,
        val catchToken: Token,
        val tryBody: Block,
        val catchVariable: Param,
        val catchBody: Block
    ) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitTryCatchStmt(this)
        }

        override fun getLine(): Int {
            return tryToken.line
        }

        override fun getFile(): String? {
            return tryToken.file
        }

    }

    data class Throw(val keyword: Token, val expr: Expr) : Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitThrowStmt(this)
        }

        override fun getLine(): Int {
            return expr.getLine()
        }

        override fun getFile(): String? {
            return expr.getFile()
        }
    }

    data class Annotation(val name: Token): Stmt(), NamedStmt {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitAnnotationStmt(this)
        }

        override fun getLine(): Int {
            return name.line
        }

        override fun getFile(): String? {
            return name.file
        }

        override fun getNameToken(): Token {
            return name
        }
    }

    data class SuperInit(val expr: Expr.Call): Stmt() {
        override fun <R> accept(visitor: Visitor<R>): R {
            return visitor.visitSuperInitStmt(this)
        }

        override fun getLine(): Int {
            return expr.getLine()
        }

        override fun getFile(): String? {
            return expr.getFile()
        }

    }

    interface Visitor<R> {
        fun visitExprStmt(stmt: Expression): R
        fun visitPrintStmt(stmt: Print): R
        fun visitVarStmt(stmt: Var): R
        fun visitBlockStmt(stmt: Block): R
        fun visitIfStmt(stmt: If): R
        fun visitWhileStmt(stmt: While): R
        fun visitBreakStmt(stmt: Break): R
        fun visitContinueStmt(stmt: Continue): R
        fun visitFunctionStmt(stmt: Function): R
        fun visitReturnStmt(stmt: Return): R
        fun visitClassStmt(stmt: Class): R
        fun visitInterfaceStmt(stmt: Interface): R
        fun visitIncludeStmt(stmt: Include): R
        fun visitImportStmt(stmt: Import): R
        fun visitModuleStmt(stmt: Module): R
        fun visitTryCatchStmt(stmt: TryCatch): R
        fun visitThrowStmt(stmt: Throw): R
        fun visitAnnotationStmt(stmt: Annotation): R
        fun visitSuperInitStmt(stmt: SuperInit): R
    }

    interface NamedStmt : Element {
        fun getNameToken(): Token
    }

    interface Annotatable {
        fun getAnnotations(): List<Annotation>
    }

    interface GenericStmt {
        fun getTypeParams(): List<Param>
    }

    interface VirtualStmt {
        fun decompose(): List<Stmt>
    }

    abstract fun <R> accept(visitor: Visitor<R>): R
}