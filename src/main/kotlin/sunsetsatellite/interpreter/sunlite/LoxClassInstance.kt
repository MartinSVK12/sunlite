package sunsetsatellite.interpreter.sunlite

import sunsetsatellite.lang.sunlite.*


open class LoxClassInstance(val clazz: LoxClass?, val sunlite: Sunlite, val runtimeTypeParams: Map<String, Type>): LoxObject {

    val fields: MutableMap<String, LoxField> = mutableMapOf<String, LoxField>().let {
        if(clazz != null) {
            it.putAll(clazz.fieldDefaults.filter { it.value.modifier != FieldModifier.STATIC })
            it.forEach { (k,v) -> it[k] = v.copy() }
        }
        return@let it
    }
    override fun superclass(): LoxClass? {
        return clazz?.superclass()
    }

    override fun superinterfaces(): List<LoxInterface> {
        return clazz?.superinterfaces() ?: emptyList()
    }

    override fun inheritsFrom(parent: LoxObject): Boolean {
        if(clazz?.superclass == parent) return true
        if(clazz?.superclass?.inheritsFrom(parent) == true) return true

        if(clazz?.superinterfaces?.contains(parent) == true) return true
        clazz?.superinterfaces?.forEach {
            if(it.superinterfaces.contains(parent)) return true
        }
        return false
    }

    open fun name(): String {
        return clazz?.name ?: "nil"
    }

    override fun toString(): String {
        return "<object '${name()}'>"
    }

    open fun get(name: Token): Any? {
        if (fields.containsKey(name.lexeme)) {
            return fields[name.lexeme]?.value
        }

        val method: LoxFunction? = clazz?.findMethod(name.lexeme)
        if (method != null) return method.bind(this)

        throw LoxRuntimeError(
            name,
            "Undefined property '" + name.lexeme + "'."
        )
    }

    open fun dynamicGet(name: String, token: Token): Any? {
        if (fields.containsKey(name)) {
            return fields[name]?.value
        }

        val method: LoxFunction? = clazz?.findMethod(name)
        if (method != null) return method.bind(this)

        return null
    }

    open fun set(name: Token, value: Any?) {
        if (fields.containsKey(name.lexeme)) {
            val field = fields[name.lexeme]!!
            var concreteType: Type = field.type
            if(field.type is Type.Parameter){
                if(runtimeTypeParams.containsKey(field.type.name.lexeme)){
                    concreteType = runtimeTypeParams[field.type.name.lexeme]!!
                } else {
                    throw LoxRuntimeError(
                        name,
                        "Undefined type parameter '" + field.type + "'."
                    )
                }
            }
            sunlite.typeChecker.checkType(concreteType, Type.fromValue(value, sunlite),name,true)
            field.value = value
            return
        }

        throw LoxRuntimeError(
            name,
            "Undefined property '" + name.lexeme + "'."
        )
    }

    open fun dynamicSet(name: String, value: Any?, token: Token) {

        if(clazz?.modifier != ClassModifier.DYNAMIC) throw LoxRuntimeError(token, "Only 'dynamic' classes support dynamic setters.")

        fields[name]?.value = value
        fields.computeIfAbsent(name) { LoxField(Type.NULLABLE_ANY, if(this is LoxClass) FieldModifier.STATIC else FieldModifier.NORMAL, null, value) }
    }


}