package sunsetsatellite.vm.lang.lox

import sunsetsatellite.vm.lang.lox.Expr.Logical
import sunsetsatellite.vm.lang.lox.Lox.runtimeError
import sunsetsatellite.vm.lang.lox.Stmt.Return
import sunsetsatellite.vm.lang.lox.TokenType.*


class Interpreter: Expr.Visitor<Any?>, Stmt.Visitor<Unit> {
	val globals: Environment = Environment()
	var environment: Environment = globals

	init {
		globals.define("clock", object: LoxCallable {
			override fun call(interpreter: Interpreter, arguments: List<Any?>?): Any {
				return System.currentTimeMillis().toDouble() / 1000.0
			}

			override fun arity(): Int {
				return 0
			}

			override fun toString(): String {
				return "<native fn>"
			}
		})
	}

	override fun visitBinaryExpr(expr: Expr.Binary): Any? {
		val left = evaluate(expr.left)
		val right = evaluate(expr.right)

		return when (expr.operator.type) {
			MINUS -> {
				checkBinaryNumberCast(expr.operator, left, right)
				left as Double - right as Double
			}
			SLASH -> {
				checkBinaryNumberCast(expr.operator, left, right)
				left as Double / right as Double
			}
			STAR -> {
				checkBinaryNumberCast(expr.operator, left, right)
				left as Double * right as Double
			}
			PLUS -> {
				if(left is String || right is String) {
					return stringify(left) + stringify(right)
				} else if(left is Double && right is Double) {
					return left + right
				} else {
					throw LoxRuntimeError(expr.operator, "Operands must be either two numbers, two strings or a string and number.")
				}
			}
			GREATER -> {
				checkBinaryNumberCast(expr.operator, left, right)
				(left as Double) > (right as Double)
			}
			GREATER_EQUAL -> {
				checkBinaryNumberCast(expr.operator, left, right)
				(left as Double) >= (right as Double)
			}
			LESS -> {
				checkBinaryNumberCast(expr.operator, left, right)
				(left as Double) < (right as Double)
			}
			LESS_EQUAL -> {
				checkBinaryNumberCast(expr.operator, left, right)
				(left as Double) <= right as Double
			}
			BANG_EQUAL -> {
				!isEqual(left, right)
			}
			EQUAL_EQUAL -> {
				isEqual(left, right)
			}
			else -> {
				null
			}
		}
	}

	override fun visitGroupingExpr(expr: Expr.Grouping): Any? {
		return evaluate(expr.expression)
	}

	override fun visitUnaryExpr(expr: Expr.Unary): Any? {
		val right = evaluate(expr.right)

		return when (expr.operator.type) {
			MINUS -> {
				checkUnaryNumberCast(expr.operator, right)
				-(right as Double)
			}
			BANG -> {
				!isTruthy(right)
			}
			else -> {
				null
			}
		}
	}

	override fun visitLiteralExpr(expr: Expr.Literal): Any? {
		return expr.value
	}

	override fun visitVariableExpr(expr: Expr.Variable): Any? {
		return environment.get(expr.name)
	}

	override fun visitAssignExpr(expr: Expr.Assign): Any? {
		val value = evaluate(expr.value)
		environment.assign(expr.name, value)
		return value
	}

	override fun visitLogicalExpr(expr: Logical): Any? {
		val left = evaluate(expr.left)

		if (expr.operator.type == OR) {
			if (isTruthy(left)) return left
		} else {
			if (!isTruthy(left)) return left
		}

		return evaluate(expr.right)
	}

	fun evaluate(expr: Expr?): Any? {
		return expr?.accept(this)
	}

	private fun isTruthy(obj: Any?): Boolean {
		if (obj == null) return false
		if (obj is Boolean) return obj
		return true
	}

	private fun isEqual(a: Any?, b: Any?): Boolean {
		if (a == null && b == null) return true
		if (a == null) return false

		if((a is Double && a.isNaN()) || (b is Double && b.isNaN())) return false

		return a == b
	}

	private fun checkUnaryNumberCast(operator: Token, operand: Any?) {
		if(operand is Double) return
		throw LoxRuntimeError(operator, "Operand must be a number.")
	}

	private fun checkBinaryNumberCast(operator: Token, left: Any?, right: Any?) {
		if(left is Double && right is Double) return
		throw LoxRuntimeError(operator, "Operands must be a numbers.")
	}

	fun interpret(statements: List<Stmt>) {
		try {
			for (statement in statements) {
				execute(statement)
			}
		} catch (error: LoxRuntimeError) {
			runtimeError(error)
		}
	}

	private fun execute(stmt: Stmt) {
		stmt.accept(this)
	}

	fun stringify(obj: Any?): String {
		if (obj == null) return "nil"

		if (obj is Double) {
			var text = obj.toString()
			if (text.endsWith(".0")) {
				text = text.substring(0, text.length - 2)
			}
			return text
		}

		return obj.toString()
	}

	override fun visitExprStmt(stmt: Stmt.Expression) {
		evaluate(stmt.expr)
	}

	override fun visitPrintStmt(stmt: Stmt.Print) {
		val evaluated = evaluate(stmt.expr)
		println(stringify(evaluated))
	}

	override fun visitVarStmt(stmt: Stmt.Var) {
		var value: Any? = null
		if (stmt.initializer != null) {
			value = evaluate(stmt.initializer)
		}

		environment.define(stmt.name.lexeme, value)
	}

	override fun visitBlockStmt(stmt: Stmt.Block) {
		executeBlock(stmt.statements, Environment(environment));
	}

	override fun visitIfStmt(stmt: Stmt.If) {
		if (isTruthy(evaluate(stmt.condition))) {
			execute(stmt.thenBranch)
		} else if (stmt.elseBranch != null) {
			execute(stmt.elseBranch)
		}
	}

	override fun visitWhileStmt(stmt: Stmt.While) {
		while (isTruthy(evaluate(stmt.condition))) {
			try {
				execute(stmt.body)
			} catch (_: LoxBreak) {
				break
			} catch (_: LoxContinue){
				if(stmt.body is Stmt.Block){
					if(stmt.body.statements[0] is Stmt.Block){
						execute(stmt.body.statements.last())
						continue
					}
				}
				continue
			}
		}
	}

	override fun visitBreakStmt(stmt: Stmt.Break) {
		throw LoxBreak()
	}

	override fun visitContinueStmt(stmt: Stmt.Continue) {
		throw LoxContinue()
	}

	override fun visitFunctionStmt(stmt: Stmt.Function) {
		val function = LoxFunction(stmt, environment)
		environment.define(stmt.name.lexeme, function)
	}

	override fun visitReturnStmt(stmt: Return) {
		var value: Any? = null
		if (stmt.value != null) value = evaluate(stmt.value)

		throw LoxReturn(value)
	}

	override fun visitCallExpr(expr: Expr.Call): Any? {
		val callee = evaluate(expr.callee)

		val arguments: MutableList<Any?> = ArrayList()
		for (argument in expr.arguments) {
			arguments.add(evaluate(argument))
		}


		if (callee !is LoxCallable) {
			throw LoxRuntimeError(
				expr.paren,
				"Can only call functions and classes."
			)
		}

		val function: LoxCallable = callee

		if (arguments.size != function.arity()) {
			throw LoxRuntimeError(
				expr.paren,
				"Expected ${function.arity()} arguments but got ${arguments.size}."
			)
		}

		return function.call(this, arguments)
	}

	override fun visitLambdaExpr(expr: Expr.Lambda): Any {
		return LoxFunction(expr.function, environment)
	}

	internal fun executeBlock(statements: List<Stmt>, environment: Environment) {
		val previous = this.environment
		try {
			this.environment = environment

			for (statement in statements) {
				execute(statement)
			}
		} finally {
			this.environment = previous
		}
	}


}