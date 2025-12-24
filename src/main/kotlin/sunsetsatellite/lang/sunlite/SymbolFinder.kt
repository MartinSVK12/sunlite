package sunsetsatellite.lang.sunlite

import kotlin.math.abs

class SymbolFinder(val name: String?, val line: Int, val column: Int): Expr.Visitor<Unit>, Stmt.Visitor<Unit> {

	var found: Element? = null

	fun find(statements: List<Stmt>): Element? {
		statements.forEach { it.accept(this) }
		return found
	}

	override fun visitExprStmt(stmt: Stmt.Expression) {
		stmt.expr.accept(this)
	}

	override fun visitPrintStmt(stmt: Stmt.Print) {
		// nothing to collect
	}

	override fun visitVarStmt(stmt: Stmt.Var) {
		stmt.initializer?.accept(this)
	}

	override fun visitBlockStmt(stmt: Stmt.Block) {
		stmt.statements.forEach { it.accept(this) }
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
		//stmt.params.forEach { addVariable(it.token, it.type) }
		stmt.body.forEach { it.accept(this) }
	}

	override fun visitReturnStmt(stmt: Stmt.Return) {
		stmt.value?.accept(this)
	}

	override fun visitClassStmt(stmt: Stmt.Class) {
		stmt.superclass?.let {
			it.accept(this)
		}
		stmt.fieldDefaults.forEach { it.accept(this) }
		stmt.methods.forEach { it.accept(this) }
	}

	override fun visitInterfaceStmt(stmt: Stmt.Interface) {
		stmt.methods.forEach { it.accept(this) }
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
		if((expr.name.lexeme == name || name == null) && expr.name.line == line){
			if(name == null){
				if(found != null && found is Expr.NamedExpr){
					if(abs(expr.name.pos.start - column) < abs((found as Expr.NamedExpr).getNameToken().pos.start - column)){
						found = expr
					}
				}
			}
			found = expr
		}
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
		if((expr.name.lexeme == name || name == null) && expr.name.line == line){
			if(name == null){
				if(found != null && found is Expr.NamedExpr){
					if(abs(expr.name.pos.start - column) < abs((found as Expr.NamedExpr).getNameToken().pos.start - column)){
						found = expr
					}
				}
			}
			found = expr
		}
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

	override fun visitArrayExpr(expr: Expr.Array) {
		expr.expr.forEach { it.accept(this) }
	}


}