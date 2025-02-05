package sunsetsatellite.lang.lox


object AstPrinter : Expr.Visitor<String>, Stmt.Visitor<String> {

	var tabs: Int = 0

	fun print(stmt: Stmt?): String {
		return stmt?.accept(this) ?: "(no valid statement)"
	}

	fun print(expr: Expr?): String {
		return expr?.accept(this) ?: "(no valid expression)"
	}

	override fun visitBinaryExpr(expr: Expr.Binary): String {
		return parenthesize(
			expr.operator.lexeme,
			expr.left, expr.right
		)
	}

	override fun visitGroupingExpr(expr: Expr.Grouping): String {
		return parenthesize("group", expr.expression)
	}

	override fun visitLiteralExpr(expr: Expr.Literal): String {
		if (expr.value == null) return "nil"
		return "\"${expr.value}\""
	}

	override fun visitVariableExpr(expr: Expr.Variable): String {
		return "(var ${expr.name.lexeme})"
	}

	override fun visitAssignExpr(expr: Expr.Assign): String {
		return "(assignment ${expr.name} -> ${expr.value.accept(this)})"
	}

	override fun visitLogicalExpr(expr: Expr.Logical): String {
		return "(logical ${print(expr.left)} ${expr.operator.lexeme} ${print(expr.right)})"
	}

	override fun visitCallExpr(expr: Expr.Call): String {
		return "(call ${print(expr.callee)} ${if (expr.arguments.isEmpty()) "(no args)" else parenthesizeList("args", expr.arguments)})"
	}

	override fun visitLambdaExpr(expr: Expr.Lambda): String {
		return parenthesize("lambda ( ${expr.function.params.toString().replace("[","").replace("]","")})", expr.function.body)
	}

	override fun visitGetExpr(expr: Expr.Get): String {
		return "(get '${expr.name.lexeme}' ${print(expr.obj)})"
	}

	override fun visitDynamicGetExpr(expr: Expr.DynamicGet): String {
		return "(dynamic get '${print(expr.what)}' ${print(expr.obj)})"
	}

	override fun visitDynamicSetExpr(expr: Expr.DynamicSet): String {
		return "(set '${print(expr.what)}' to ${print(expr.value)} on ${print(expr.obj)})"
	}

	override fun visitSetExpr(expr: Expr.Set): String {
		return "(set '${expr.name.lexeme}' to ${print(expr.value)} on ${print(expr.obj)})"
	}

	override fun visitThisExpr(expr: Expr.This): String {
		return "(this)"
	}

	override fun visitSuperExpr(expr: Expr.Super): String {
		return "(super)"
	}

	override fun visitCheckExpr(expr: Expr.Check): String {
		return "(check ${print(expr.left)} ${expr.operator.lexeme} ${expr.right.getName()})"
	}

	override fun visitCastExpr(expr: Expr.Cast): String {
		return "(cast ${print(expr.left)} ${expr.operator.lexeme} ${expr.right.getName()})"
	}

	override fun visitUnaryExpr(expr: Expr.Unary): String {
		return parenthesize(expr.operator.lexeme, expr.right)
	}

	private fun parenthesize(name: String, vararg exprs: Expr): String {
		val builder = StringBuilder()

		builder.append("(").append(name)
		for ((i, expr) in exprs.withIndex()) {
			builder.append(" ")
			builder.append(expr.accept(this))
			if(exprs.size-1 > i) builder.append(",")
		}
		builder.append(")")

		return builder.toString()
	}

	private fun parenthesizeList(name: String, exprs: List<Expr>): String {
		val builder = StringBuilder()

		builder.append("(").append(name)
		for ((i, expr) in exprs.withIndex()) {
			builder.append(" ")
			builder.append(expr.accept(this))
			if(exprs.size-1 > i) builder.append(",")
		}
		builder.append(")")

		return builder.toString()
	}

	private fun parenthesize(name: String, stmts: List<Stmt>): String {
		val builder = StringBuilder()

		builder.append("(").append(name)
		if(name == "block" || (name.contains("function") && !name.contains("abstract") && !name.contains("native")) || name.contains("class") || name.contains("interface") || name.contains("lambda")) {
			builder.append(" {\n")
			tabs++
		}
		for ((i, stmt) in stmts.withIndex()) {
			builder.append("\t".repeat(tabs))
			builder.append(stmt.accept(this))
			if(stmts.size-1 > i) builder.append(",\n")
		}
		if(name == "block" || (name.contains("function") && !name.contains("abstract") && !name.contains("native")) || name.contains("class") || name.contains("interface") || name.contains("lambda")){
			tabs--
			builder.append("\n").append("\t".repeat(tabs)).append("}")
		}
		builder.append(")")

		return builder.toString()
	}

	private fun parenthesize(name: String, expr: Expr?, stmts: List<Stmt>): String {
		val builder = StringBuilder()

		builder.append("(").append(name)
		if(expr != null) builder.append(" [condition ").append(expr.accept(this)).append("] {\n")
		tabs++
		for ((i, stmt) in stmts.withIndex()) {
			builder.append("\t".repeat(tabs))
			builder.append(stmt.accept(this))
			if(stmts.size-1 > i) builder.append(",\n")
		}
		tabs--
		builder.append("\n").append("\t".repeat(tabs)).append("}")
		builder.append(")")

		return builder.toString()
	}

	override fun visitExprStmt(stmt: Stmt.Expression): String {
		return print(stmt.expr)
	}

	override fun visitPrintStmt(stmt: Stmt.Print): String {
		return parenthesize("print", stmt.expr)
	}

	override fun visitVarStmt(stmt: Stmt.Var): String {
		return stmt.initializer?.let { parenthesize("${stmt.modifier.name.lowercase()} var '${stmt.name.lexeme}' (type '${stmt.type.getName().lowercase()}')", it) } ?: "(${stmt.modifier.name.lowercase()} var '${stmt.name.lexeme}' (type '${stmt.type.getName().lowercase()}') nil)"
	}

	override fun visitBlockStmt(stmt: Stmt.Block): String {
		return parenthesize("block",stmt.statements)
	}

	override fun visitIfStmt(stmt: Stmt.If): String {
		return parenthesize("if", stmt.condition, listOf(stmt.thenBranch))
	}

	override fun visitWhileStmt(stmt: Stmt.While): String {
		return parenthesize("while", stmt.condition, listOf(stmt.body))
	}

	override fun visitBreakStmt(stmt: Stmt.Break): String {
		return "(break)"
	}

	override fun visitContinueStmt(stmt: Stmt.Continue): String {
		return "(continue)"
	}

	override fun visitFunctionStmt(stmt: Stmt.Function): String {
		return parenthesize("${stmt.modifier.name.lowercase()} function ${stmt.name.lexeme}( ${stmt.params.toString().replace("[","").replace("]","")} ): ${stmt.returnType.getName()}", stmt.body)
	}

	override fun visitReturnStmt(stmt: Stmt.Return): String {
		return "(return ${print(stmt.value)})"
	}

	override fun visitClassStmt(stmt: Stmt.Class): String {
		return parenthesize("${stmt.modifier.name.lowercase()} class${if (stmt.typeParameters.isEmpty()) "" else "<${stmt.typeParameters.joinToString(",")}>"} ${stmt.name.lexeme}${stmt.superclass?.let { return@let " < (superclass ${it.name.lexeme})" } ?: ""}${if(stmt.superinterfaces.isNotEmpty()) " << ${parenthesizeList("superinterfaces",stmt.superinterfaces)}" else ""}",
			mutableListOf<Stmt>().let { it.addAll(stmt.fieldDefaults); it.addAll(stmt.methods); return@let it }.toList())
	}

	override fun visitInterfaceStmt(stmt: Stmt.Interface): String {
		return parenthesize("interface${if (stmt.typeParameters.isEmpty()) "" else "<${stmt.typeParameters.joinToString(",")}>"} ${stmt.name.lexeme}${if(stmt.superinterfaces.isNotEmpty()) " << ${parenthesizeList("superinterfaces",stmt.superinterfaces)}" else ""}",stmt.methods)
	}

	override fun visitImportStmt(stmt: Stmt.Import): String {
		return "(import ${stmt.what.lexeme})"
	}
}