package sunsetsatellite.lang.lox

import sunsetsatellite.lang.lox.Expr.Logical
import sunsetsatellite.lang.lox.Stmt.Return
import sunsetsatellite.lang.lox.TokenType.*
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets


class Interpreter(file: String?, val globals: Environment = Environment(null,0, "<global env>", file), val lox: Lox): Expr.Visitor<Any?>, Stmt.Visitor<Unit> {
	var environment: Environment = globals
	val locals: MutableMap<Expr, Int> = mutableMapOf()
	var currentFile: String? = null
	var continueExecution: Boolean = false
	var breakpointHit: Boolean = false

	private val stdin: InputStreamReader = InputStreamReader(System.`in`, StandardCharsets.UTF_8)

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
			IS -> {
				inheritsFrom(expr.operator, left, right)
			}
			IS_NOT -> {
				!inheritsFrom(expr.operator, left, right)
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
		return lookUpVariable(expr.name, expr)
	}

	override fun visitAssignExpr(expr: Expr.Assign): Any? {
		val value = evaluate(expr.value)

		val distance = locals[expr]
		if (distance != null) {
			environment.assignAt(distance, expr.name, value)
		} else {
			globals.assign(expr.name, value)
		}

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
		expr?.let { environment.line = it.getLine() }
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
		throw LoxRuntimeError(operator, "Operands must be numbers.")
	}

	private fun inheritsFrom(operator: Token, left: Any?, right: Any?): Boolean {
		if(left is LoxObject && right is LoxObject){
			return left.inheritsFrom(right)
		}
		throw LoxRuntimeError(operator, "Operands must be classes or interfaces.")
	}

	fun interpret(statements: List<Stmt>, path: String?) {
		currentFile = path
		try {
			for (statement in statements) {
				execute(statement)
			}
		} catch (error: LoxRuntimeError) {
			lox.runtimeError(error)
		}
	}

	private fun execute(stmt: Stmt) {
		environment.line = stmt.getLine()
		currentFile = stmt.getFile()
		if(lox.breakpoints[currentFile]?.contains(stmt.getLine()) == true){
			lox.breakpointListeners.forEach { it.breakpointHit(stmt.getLine(),currentFile,lox,environment) }
			breakpointHit = true
			while (true){
				println(continueExecution)
				if(continueExecution) {
					breakpointHit = false
					continueExecution = false
					break
				}
			}
		}
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
		executeBlock(stmt.statements, Environment(environment, stmt.getLine(), "<block>", stmt.getFile()));
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
		val function = LoxFunction(stmt, environment, lox)
		environment.define(stmt.name.lexeme, function)
	}

	override fun visitReturnStmt(stmt: Return) {
		var value: Any? = null
		if (stmt.value != null) value = evaluate(stmt.value)

		throw LoxReturn(value)
	}

	override fun visitClassStmt(stmt: Stmt.Class) {

		var superclass: Any? = null
		if (stmt.superclass != null) {
			superclass = evaluate(stmt.superclass)
			if (superclass !is LoxClass) {
				throw LoxRuntimeError(
					stmt.superclass.name,
					"Superclass must be a class."
				)
			}
		}

		val superinterfaces: MutableList<LoxInterface> = mutableListOf()
		stmt.superinterfaces.forEach {
			val superinterface = evaluate(it)
			if(superinterface !is LoxInterface) {
				throw LoxRuntimeError(
					it.name,
					"Superinterface must be an interface."
				)
			}
			superinterfaces.add(superinterface)
		}

		environment.define(stmt.name.lexeme, null)

		if (stmt.superclass != null) {
			environment = Environment(environment,stmt.superclass.getLine(),"super::${stmt.name.lexeme}", stmt.superclass.getFile())
			environment.define("super", superclass)
		}

		val methods: MutableMap<String, LoxFunction> = mutableMapOf()
		for (method in stmt.methods) {
			val function = LoxFunction(method, environment, lox).apply { if (method.name.lexeme == "init" && method.modifier != FunctionModifier.NATIVE) this.setModifier(FunctionModifier.INIT) }
			methods[method.name.lexeme] = function
		}

		val clazz = LoxClass(stmt.name.lexeme, methods, stmt.fieldDefaults.associate { it.name.lexeme to LoxField(it.type, it.modifier, evaluate(it.initializer)) }, superclass as LoxClass?, superinterfaces, stmt.modifier, lox)

		if (superclass != null) {
			environment = environment.enclosing!!
		}

		environment.assign(stmt.name, clazz)
	}

	override fun visitInterfaceStmt(stmt: Stmt.Interface) {

		val superinterfaces: MutableList<LoxInterface> = mutableListOf()
		stmt.superinterfaces.forEach {
			val superinterface = evaluate(it)
			if(superinterface !is LoxInterface) {
				throw LoxRuntimeError(
					it.name,
					"Superinterface must be an interface."
				)
			}
			superinterfaces.add(superinterface)
		}

		environment.define(stmt.name.lexeme, null)

		val methods: MutableMap<String, LoxFunction> = mutableMapOf()
		for (method in stmt.methods) {
			val function = LoxFunction(method, environment, lox).apply { this.setModifier(FunctionModifier.ABSTRACT) }
			methods[method.name.lexeme] = function
		}

		val clazz = LoxInterface(stmt.name.lexeme, methods, superinterfaces)

		environment.assign(stmt.name, clazz)
	}

	override fun visitImportStmt(stmt: Stmt.Import) {
		lox.imports[stmt.what.literal]?.let { interpret(it, stmt.what.literal.toString()) }
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
		return LoxFunction(expr.function, environment, lox)
	}

	override fun visitGetExpr(expr: Expr.Get): Any? {
		val obj = evaluate(expr.obj)
		if (obj is LoxClassInstance) {
			return obj.get(expr.name)
		}

		if (obj is LoxInterface){
			throw LoxRuntimeError(
				expr.name,
				"Can't get abstract method '${expr.name.lexeme}' on interface '${obj.name}'"
			)
		}

		throw LoxRuntimeError(
			expr.name,
			"Only classes or class instances have properties."
		)
	}

	override fun visitDynamicGetExpr(expr: Expr.DynamicGet): Any? {
		val obj = evaluate(expr.obj)
		val what: String = evaluate(expr.what).toString()

		if (obj is LoxClassInstance) {
			return obj.dynamicGet(what, expr.token)
		}

		if (obj is LoxInterface){
			throw LoxRuntimeError(
				expr.token,
				"Can't get abstract method '${expr.what}' on interface '${obj.name}'"
			)
		}

		throw LoxRuntimeError(
			expr.token,
			"Only classes or class instances have properties."
		)
	}

	override fun visitSetExpr(expr: Expr.Set): Any? {
		val obj = evaluate(expr.obj) as? LoxClassInstance ?: throw LoxRuntimeError(
			expr.name,
			"Only classes or class instances have fields."
		)

		val value = evaluate(expr.value)
		obj.set(expr.name, value)
		return value
	}

	override fun visitDynamicSetExpr(expr: Expr.DynamicSet): Any? {
		val obj = evaluate(expr.obj) as? LoxClassInstance ?: throw LoxRuntimeError(
			expr.token,
			"Only classes or class instances have fields."
		)

		val what = evaluate(expr.what).toString()

		val value = evaluate(expr.value)
		obj.dynamicSet(what, value, expr.token)
		return value
	}

	override fun visitThisExpr(expr: Expr.This): Any? {
		return lookUpVariable(expr.keyword, expr)
	}

	override fun visitSuperExpr(expr: Expr.Super): Any? {
		val distance = locals[expr]!!
		val superclass = environment.getAt(distance, "super") as LoxClass?

		//env with `this` is always in the previous env
		val obj: LoxClassInstance = environment.getAt(distance - 1, "this") as LoxClassInstance

		val method = superclass?.findMethod(expr.method.lexeme)
			?: throw LoxRuntimeError(expr.method,
			"Undefined property '" + expr.method.lexeme + "'.")

		return method.bind(obj)
	}

	override fun visitCheckExpr(expr: Expr.Check): Any? {
		val left = evaluate(expr.left)
		val right = expr.right

		return when (expr.operator.type) {
			IS -> inheritsFrom(expr.operator, left, right)
			IS_NOT -> !inheritsFrom(expr.operator, left, right)
			else -> null
		}
	}

	override fun visitCastExpr(expr: Expr.Cast): Any? {
		val evaluated = evaluate(expr.left)
		val type = Type.fromValue(evaluated,lox)

		lox.typeChecker.checkType(type,expr.right,expr.operator,true)

		return evaluated
	}


	fun executeBlock(statements: List<Stmt>, environment: Environment) {
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

	fun resolve(expr: Expr, depth: Int) {
		locals[expr] = depth
	}

	private fun lookUpVariable(name: Token, expr: Expr): Any? {
		val distance = locals[expr]
		return if (distance != null) {
			environment.getAt(distance, name.lexeme)
		} else {
			globals.get(name)
		}
	}
}