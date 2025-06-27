package sunsetsatellite.vm.sunlite

class SLClosure(val function: SLFunction, val upvalues: Array<SLUpvalue?> = arrayOfNulls(function.upvalueCount)) {

	override fun toString(): String {
		return function.toString()
	}

	fun copy(): SLClosure {
		return SLClosure(function, upvalues.map { it?.copy() }.toTypedArray())
	}
}