package sunsetsatellite.lang.lox

class LoxInterface(val name: String, val methods: MutableMap<String, LoxFunction>, val superinterfaces: List<LoxInterface>) : LoxObject {

    fun findMethod(name: String): LoxFunction? {
        if (methods.containsKey(name)) {
            return methods[name]
        }

        superinterfaces.forEach {
            val method = it.findMethod(name)
            if (method != null) return method
        }

        return null
    }

    override fun superclass(): LoxClass? {
        return null
    }

    override fun superinterfaces(): List<LoxInterface> {
        return superinterfaces
    }

    override fun inheritsFrom(parent: LoxObject): Boolean {
        if(superinterfaces.contains(parent)) return true
        superinterfaces.forEach {
            if(it.superinterfaces.contains(parent)) return true
        }
        return false
    }

    override fun toString(): String {
        return "<interface '${name}'>"
    }

}