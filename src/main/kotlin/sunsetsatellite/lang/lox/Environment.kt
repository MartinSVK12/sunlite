package sunsetsatellite.lang.lox

class Environment(val enclosing: Environment? = null, var line: Int = -1, val name: String, val file: String?) {

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

	fun getAt(distance: Int, name: String?): Any? {
		return ancestor(distance)?.values?.get(name)
	}

	fun ancestor(distance: Int): Environment? {
		var environment: Environment? = this
		for (i in 0..<distance) {
			environment = environment?.enclosing
		}

		return environment
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

	fun assignAt(distance: Int, name: Token, value: Any?) {
		ancestor(distance)?.values?.set(name.lexeme, value
			?: throw LoxRuntimeError(
				name,
				"Undefined variable '" + name.lexeme + "'."
			)
		)
	}

	override fun toString(): String {
		return "${name}(${file}:${line}) (${values.size} values)"
	}
}