package sunsetsatellite.vm.sunlite

class SunliteClosure(val function: SunliteFunction, val upvalues: Array<SunliteUpvalue?> = arrayOfNulls(function.upvalueCount)) {

	override fun toString(): String {
		return function.toString()
	}
}