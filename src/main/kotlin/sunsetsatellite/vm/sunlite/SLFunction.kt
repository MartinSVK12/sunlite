package sunsetsatellite.vm.sunlite

import sunsetsatellite.lang.sunlite.Param
import sunsetsatellite.lang.sunlite.Type

class SLFunction(val name: String, val returnType: Type, val params: List<Param>, val chunk: Chunk, val arity: Int = 0, val upvalueCount: Int, val localsCount: Int) {

	override fun toString(): String {
		return "<function '${name}(${params.map { it.type }.joinToString()}): ${returnType}'>"
	}

	fun copy(): SLFunction {
		return SLFunction(name, returnType, params, chunk, arity, upvalueCount, localsCount)
	}
}