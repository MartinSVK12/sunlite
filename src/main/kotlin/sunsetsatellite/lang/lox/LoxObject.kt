package sunsetsatellite.lang.lox

interface LoxObject {
    fun superclass(): LoxClass?
    fun superinterfaces(): List<LoxInterface>

    fun inheritsFrom(parent: LoxObject): Boolean
}