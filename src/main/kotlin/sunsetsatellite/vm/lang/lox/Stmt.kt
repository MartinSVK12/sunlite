package sunsetsatellite.vm.lang.lox

abstract class Stmt {

	data class Expression(val expr: Expr) : Stmt() {
		override fun <R> accept(visitor: Visitor<R>): R {
			return visitor.visitExprStmt(this)
		}
	}

	data class Print(val expr: Expr) : Stmt() {
		override fun <R> accept(visitor: Visitor<R>): R {
			return visitor.visitPrintStmt(this)
		}
	}

	data class Var(val name: Token, val initializer: Expr?) : Stmt() {
		override fun <R> accept(visitor: Visitor<R>): R {
			return visitor.visitVarStmt(this)
		}
	}

	data class Block(val statements: List<Stmt>) : Stmt() {
		override fun <R> accept(visitor: Visitor<R>): R {
			return visitor.visitBlockStmt(this)
		}
	}

	data class If(val condition: Expr, val thenBranch: Stmt, val elseBranch: Stmt?) : Stmt() {
		override fun <R> accept(visitor: Visitor<R>): R {
			return visitor.visitIfStmt(this)
		}
	}

	data class While(val condition: Expr, val body: Stmt) : Stmt() {
		override fun <R> accept(visitor: Visitor<R>): R {
			return visitor.visitWhileStmt(this)
		}
	}

	class Break : Stmt(){
		override fun <R> accept(visitor: Visitor<R>): R {
			return visitor.visitBreakStmt(this)
		}
	}

	class Continue : Stmt(){
		override fun <R> accept(visitor: Visitor<R>): R {
			return visitor.visitContinueStmt(this)
		}
	}

	data class Function(val name: Token, val params: List<Token>, val body: List<Stmt>) : Stmt(){
		override fun <R> accept(visitor: Visitor<R>): R {
			return visitor.visitFunctionStmt(this)
		}
	}

	data class Return(val keyword: Token, val value: Expr?) : Stmt(){
		override fun <R> accept(visitor: Visitor<R>): R {
			return visitor.visitReturnStmt(this)
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
	}

	abstract fun <R> accept(visitor: Visitor<R>): R
}