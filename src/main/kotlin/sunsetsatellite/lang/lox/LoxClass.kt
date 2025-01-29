package sunsetsatellite.lang.lox


class LoxClass(val name: String, val methods: MutableMap<String, LoxFunction>, val superclass: LoxClass?): LoxClassInstance(null), LoxCallable {
    override fun call(interpreter: Interpreter, arguments: List<Any?>?): Any {
        val instance = LoxClassInstance(this)

        val initializer = findMethod("init")
        initializer?.bind(instance)?.call(interpreter, arguments)

        return instance
    }

    override fun arity(): Int {
        val initializer = findMethod("init") ?: return 0
        return initializer.arity()
    }

    fun findMethod(name: String): LoxFunction? {
        if (methods.containsKey(name)) {
            return methods[name]
        }

        if (superclass != null) {
            return superclass.findMethod(name);
        }

        return null
    }

    override fun get(name: Token): Any? {
        if (fields.containsKey(name.lexeme)) {
            return fields[name.lexeme]
        }

        val method: LoxFunction? = findMethod(name.lexeme)
        if (method != null && method.declaration.modifier == FunctionModifier.STATIC) return method

        throw LoxRuntimeError(
            name,
            "Undefined property '" + name.lexeme + "'."
        )
    }

    override fun toString(): String {
        return name
    }
}