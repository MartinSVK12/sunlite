package sunsetsatellite.vm.lang.lox

class LoxFunction(val declaration: Stmt.Function, val closure: Environment): LoxCallable {

	override fun call(
		interpreter: Interpreter,
		arguments: List<Any?>?
	): Any? {
		val environment = Environment(closure)
		for (i in declaration.params.indices) {
			environment.define(
				declaration.params[i].lexeme,
				arguments?.get(i)
			)
		}

		try {
			interpreter.executeBlock(declaration.body, environment)
		} catch (returnValue: LoxReturn) {
			return returnValue.value
		}
		return null
	}

	override fun arity(): Int {
		return declaration.params.size
	}

	override fun toString(): String {
		return "<fn '${declaration.name.lexeme}'>"
	}

}