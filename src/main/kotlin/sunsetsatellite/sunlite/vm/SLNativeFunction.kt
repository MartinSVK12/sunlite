package sunsetsatellite.sunlite.vm

import sunsetsatellite.sunlite.lang.Type

abstract class SLNativeFunction(val name: String, val returnType: Type, val arity: Int = 0) {

    override fun toString(): String {
        return "<native fn '${name}'>"
    }

    abstract fun call(vm: VM, args: Array<AnySLValue>): AnySLValue
}