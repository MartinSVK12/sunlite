package sunsetsatellite.lang.lox


class LoxClass(
    val name: String,
    val methods: MutableMap<String, LoxFunction>,
    val fieldDefaults: Map<String, LoxField>,
    val superclass: LoxClass?,
    val superinterfaces: List<LoxInterface>,
    val modifier: ClassModifier,
    lox: Lox
): LoxClassInstance(null, lox), LoxCallable, LoxObject {

     val staticFields = mutableMapOf<String, LoxField>().let {
        it.putAll(fieldDefaults.filter { default -> default.value.modifier == FieldModifier.STATIC })
        it.forEach { (k,v) -> it[k] = v.copy() }
        return@let it
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>?): Any {
        val instance = LoxClassInstance(this, lox)

        val initializer = findMethod("init")
        initializer?.bind(instance)?.call(interpreter, arguments)

        return instance
    }

    override fun arity(): Int {
        val initializer = findMethod("init") ?: return 0
        return initializer.arity()
    }

    override fun signature(): String {
        return name
    }

    fun findMethod(name: String): LoxFunction? {
        if (methods.containsKey(name)) {
            return methods[name]
        }

        if (superclass != null) {
            return superclass.findMethod(name);
        }

        superinterfaces.forEach {
            val method = it.findMethod(name)
            if (method != null) throw LoxRuntimeError(
                method.declaration.name,
                "Attempted to access unimplemented abstract method '${name}'."
            )
        }

        return null
    }

    override fun name(): String {
        return name
    }

    override fun superclass(): LoxClass? {
        return superclass
    }

    override fun superinterfaces(): List<LoxInterface> {
        return superinterfaces
    }

    override fun inheritsFrom(parent: LoxObject): Boolean {
        if(superclass == parent) return true
        if(superclass?.inheritsFrom(parent) == true) return true

        if(superinterfaces.contains(parent)) return true
        superinterfaces.forEach {
            if(it.superinterfaces.contains(parent)) return true
        }
        return false
    }

    override fun get(name: Token): Any? {
        if (staticFields.containsKey(name.lexeme)) {
            return staticFields[name.lexeme]?.value
        }

        val method: LoxFunction? = findMethod(name.lexeme)
        if (method != null && method.declaration.modifier == FunctionModifier.STATIC) return method

        throw LoxRuntimeError(
            name,
            "Undefined property '" + name.lexeme + "'."
        )
    }

    override fun dynamicGet(name: String, token: Token): Any? {

        if(modifier != ClassModifier.DYNAMIC) throw LoxRuntimeError(token, "Only 'dynamic' classes support dynamic getters.")

        if (staticFields.containsKey(name)) {
            return staticFields[name]?.value
        }

        val method: LoxFunction? = findMethod(name)
        if (method != null && method.declaration.modifier == FunctionModifier.STATIC) return method

        return null
    }

    override fun set(name: Token, value: Any?) {
        if (staticFields.containsKey(name.lexeme)) {
            val field = staticFields[name.lexeme]!!
            lox.typeChecker.checkType(field.type,Type.fromValue(value,lox),name,true)
            field.value = value
            return
        }

        throw LoxRuntimeError(
            name,
            "Undefined property '" + name.lexeme + "'."
        )
    }

    override fun dynamicSet(name: String, value: Any?, token: Token) {

        if(modifier != ClassModifier.DYNAMIC) throw LoxRuntimeError(token, "Only 'dynamic' classes support dynamic setters.")

        staticFields[name]?.value = value
        staticFields.computeIfAbsent(name) { LoxField(Type.NULLABLE_ANY, FieldModifier.STATIC, null, value) }
    }

    override fun toString(): String {
        return "<class '${name}'>"
    }
}