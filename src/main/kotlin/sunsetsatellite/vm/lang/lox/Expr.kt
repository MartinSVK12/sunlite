package sunsetsatellite.vm.lang.lox

abstract class Expr {
	data class Binary(val left: Expr, val operator: Token, val right: Expr) : Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitBinaryExpr(this)
		}
	}

	data class Grouping(val expression: Expr) : Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitGroupingExpr(this)
		}
	}

	data class Unary(val operator: Token, val right: Expr) : Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitUnaryExpr(this)
		}
	}

	data class Literal(val value: Any?): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitLiteralExpr(this)
		}
	}

	data class Variable(val name: Token): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitVariableExpr(this)
		}
	}

	data class Assign(val name: Token, val value: Expr): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitAssignExpr(this)
		}
	}

	data class Logical(val left: Expr, val operator: Token, val right: Expr): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitLogicalExpr(this)
		}
	}

	data class Call(val callee: Expr, val paren: Token, val arguments: List<Expr>): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitCallExpr(this)
		}
	}

	data class Lambda(val function: Stmt.Function): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitLambdaExpr(this)
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
	}

	abstract fun <R> accept(visitor: Visitor<R>): R?
}