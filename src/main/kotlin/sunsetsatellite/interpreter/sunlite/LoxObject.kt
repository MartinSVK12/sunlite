package sunsetsatellite.interpreter.sunlite

interface LoxObject {
    fun superclass(): LoxClass?
    fun superinterfaces(): List<LoxInterface>

    fun inheritsFrom(parent: LoxObject): Boolean
}