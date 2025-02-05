package sunsetsatellite.lang.lox

abstract class Expr {
	data class Binary(val left: Expr, val operator: Token, val right: Expr) : Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitBinaryExpr(this)
		}

		override fun getLine(): Int {
			return operator.line
		}

		override fun getFile(): String? {
			return operator.file
		}
	}

	data class Grouping(val expression: Expr) : Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitGroupingExpr(this)
		}

		override fun getLine(): Int {
			return expression.getLine()
		}

		override fun getFile(): String? {
			return expression.getFile()
		}
	}

	data class Unary(val operator: Token, val right: Expr) : Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitUnaryExpr(this)
		}

		override fun getLine(): Int {
			return operator.line
		}

		override fun getFile(): String? {
			return operator.file
		}
	}

	data class Literal(val value: Any?, val lineNumber: Int, val currentFile: String?): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitLiteralExpr(this)
		}

		override fun getLine(): Int {
			return lineNumber
		}

		override fun getFile(): String? {
			return currentFile
		}
	}

	data class Variable(val name: Token): Expr(), NamedExpr {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitVariableExpr(this)
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

	/*class GenericVariable(name: Token, val typeParameters: List<Param>): Variable(name) {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitVariableExpr(this)
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
	}*/

	data class Assign(val name: Token, val value: Expr): Expr(), NamedExpr {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitAssignExpr(this)
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

	data class Logical(val left: Expr, val operator: Token, val right: Expr): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitLogicalExpr(this)
		}

		override fun getLine(): Int {
			return operator.line
		}

		override fun getFile(): String? {
			return operator.file
		}
	}

	data class Call(val callee: Expr, val paren: Token, val arguments: List<Expr>): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitCallExpr(this)
		}

		override fun getLine(): Int {
			return paren.line
		}

		override fun getFile(): String? {
			return paren.file
		}
	}

	data class Lambda(val function: Stmt.Function): Expr(), NamedExpr {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitLambdaExpr(this)
		}

		override fun getLine(): Int {
			return function.name.line
		}

		override fun getFile(): String? {
			return function.getFile()
		}

		override fun getNameToken(): Token {
			return function.name
		}
	}

	data class Get(val obj: Expr, val name: Token): Expr(), NamedExpr {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitGetExpr(this)
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

	data class DynamicGet(val obj: Expr, val what: Expr, val token: Token): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitDynamicGetExpr(this)
		}

		override fun getLine(): Int {
			return token.line
		}

		override fun getFile(): String? {
			return token.file
		}
	}

	data class Set(val obj: Expr, val name: Token, val value: Expr): Expr(), NamedExpr {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitSetExpr(this)
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

	data class DynamicSet(val obj: Expr, val what: Expr, val value: Expr, val token: Token): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitDynamicSetExpr(this)
		}

		override fun getLine(): Int {
			return token.line
		}

		override fun getFile(): String? {
			return token.file
		}
	}

	data class This(val keyword: Token): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitThisExpr(this)
		}

		override fun getLine(): Int {
			return keyword.line
		}

		override fun getFile(): String? {
			return keyword.file
		}
	}

	data class Super(val keyword: Token, val method: Token): Expr(), NamedExpr {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitSuperExpr(this)
		}

		override fun getLine(): Int {
			return keyword.line
		}

		override fun getFile(): String? {
			return keyword.file
		}

		override fun getNameToken(): Token {
			return method
		}
	}

	data class Check(val left: Expr, val operator: Token, val right: Type): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitCheckExpr(this)
		}

		override fun getLine(): Int {
			return operator.line
		}

		override fun getFile(): String? {
			return operator.file
		}
	}

	data class Cast(val left: Expr, val operator: Token, val right: Type): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitCastExpr(this)
		}

		override fun getLine(): Int {
			return operator.line
		}

		override fun getFile(): String? {
			return operator.file
		}
	}

	interface Visitor<R> {
		fun visitBinaryExpr(expr: Binary): R
		fun visitGroupingExpr(expr: Grouping): R
		fun visitUnaryExpr(expr: Unary): R
		fun visitLiteralExpr(expr: Literal): R
		fun visitVariableExpr(expr: Variable): R
		fun visitAssignExpr(expr: Assign): R
		fun visitLogicalExpr(expr: Logical): R
		fun visitCallExpr(expr: Call): R?
		fun visitLambdaExpr(expr: Lambda): R
		fun visitGetExpr(expr: Get): R
		fun visitDynamicGetExpr(expr: DynamicGet): R
		fun visitDynamicSetExpr(expr: DynamicSet): R
		fun visitSetExpr(expr: Set): R
		fun visitThisExpr(expr: This): R
		fun visitSuperExpr(expr: Super): R
		fun visitCheckExpr(expr: Check): R
		fun visitCastExpr(expr: Cast): R
	}

	interface NamedExpr {
		fun getNameToken(): Token
	}

	abstract fun <R> accept(visitor: Visitor<R>): R?

	abstract fun getLine(): Int
	abstract fun getFile(): String?
}