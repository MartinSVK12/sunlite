package sunsetsatellite.vm.sunlite

import sunsetsatellite.lang.sunlite.Type

abstract class SLNativeFunction(val name: String, val returnType: Type, val arity: Int = 0) {

	override fun toString(): String {
		return "<native fn '${name}'>"
	}

	abstract fun call(vm: VM, args: Array<AnySLValue>): AnySLValue
}