package sunsetsatellite.vm.lang.lox

class Environment(val enclosing: Environment? = null) {

	val values: MutableMap<String, Any?> = HashMap()

	fun define(name: String, value: Any?) {
		values[name] = value
	}

	fun get(name: Token): Any? {
		if (values.containsKey(name.lexeme)) {
			return values[name.lexeme]
		}

		if(enclosing != null) return enclosing.get(name)

		throw LoxRuntimeError(
			name,
			"Undefined variable '" + name.lexeme + "'."
		)
	}

	fun assign(name: Token, value: Any?) {
		if (values.containsKey(name.lexeme)) {
			values[name.lexeme] = value
			return
		}

		if (enclosing != null) {
			enclosing.assign(name, value);
			return
		}

		throw LoxRuntimeError(
			name,
			"Undefined variable '" + name.lexeme + "'."
		)
	}
}