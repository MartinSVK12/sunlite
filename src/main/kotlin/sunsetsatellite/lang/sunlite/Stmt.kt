package sunsetsatellite.lang.sunlite

abstract class Stmt: Element {

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

	data class Block(val statements: List<Stmt>, val lineNumber: Int, val currentFile: String?) : Stmt() {
		override fun <R> accept(visitor: Visitor<R>): R {
			return visitor.visitBlockStmt(this)
		}

		override fun getLine(): Int {
			return lineNumber
		}

		override fun getFile(): String? {
			return currentFile
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

	class Break(val keyword: Token) : Stmt(){
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

	class Continue(val keyword: Token) : Stmt(){
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

	data class Function(val name: Token, val params: List<Param>, val body: List<Stmt>, var modifier: FunctionModifier, val returnType: Type, val typeParams: List<Param>) : Stmt(),
		NamedStmt {
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
	}

	data class Return(val keyword: Token, val value: Expr?) : Stmt(){
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

	data class Import(val keyword: Token, val what: Token) : Stmt(){
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

	data class Class(val name: Token, val methods: List<Function>, val fieldDefaults: List<Var>, val superclass: Expr.Variable?, val superinterfaces: List<Expr.Variable>, val modifier: ClassModifier, val typeParameters: List<Param>) : Stmt(),
		NamedStmt {
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
	}

	data class Interface(val name: Token, val methods: List<Function>, val superinterfaces: List<Expr.Variable>, val typeParameters: List<Param>) : Stmt(),
		NamedStmt {
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
		fun visitImportStmt(stmt: Import): R
	}

	interface NamedStmt: Element {
		fun getNameToken(): Token
	}

	abstract fun <R> accept(visitor: Visitor<R>): R
}