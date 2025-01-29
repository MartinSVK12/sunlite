package sunsetsatellite.lang.lox


open class LoxClassInstance(private val clazz: LoxClass?) {

    protected val fields: MutableMap<String, Any?> = mutableMapOf()

    override fun toString(): String {
        return "instance of ${clazz?.name}"
    }

    open fun get(name: Token): Any? {
        if (fields.containsKey(name.lexeme)) {
            return fields[name.lexeme]
        }

        val method: LoxFunction? = clazz?.findMethod(name.lexeme)
        if (method != null) return method.bind(this)

        throw LoxRuntimeError(
            name,
            "Undefined property '" + name.lexeme + "'."
        )
    }

    fun set(name: Token, value: Any?) {
        fields[name.lexeme] = value
    }


}