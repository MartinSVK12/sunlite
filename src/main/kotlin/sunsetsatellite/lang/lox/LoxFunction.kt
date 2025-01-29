package sunsetsatellite.lang.lox

class LoxFunction(
	val declaration: Stmt.Function,
	val closure: Environment,
) : LoxCallable {

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
			if (declaration.modifier == FunctionModifier.INIT) return closure.getAt(0, "this");
			return returnValue.value
		}

		if (declaration.modifier == FunctionModifier.INIT) return closure.getAt(0, "this");

		return null
	}

	override fun arity(): Int {
		return declaration.params.size
	}

	fun bind(instance: LoxClassInstance): LoxFunction {
		if(declaration.modifier == FunctionModifier.STATIC) {
			throw LoxRuntimeError(declaration.name, "Cannot bind 'this' to a static function.")
		}
		val environment = Environment(closure)
		environment.define("this", instance)
		return LoxFunction(declaration, environment)
	}


	override fun toString(): String {
		return "<${declaration.modifier.name.lowercase()} fn '${declaration.name.lexeme}'>"
	}

	fun setModifier(modifier: FunctionModifier): LoxFunction {
		declaration.modifier = modifier
		return this
	}

}