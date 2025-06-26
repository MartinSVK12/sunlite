package sunsetsatellite.vm.sunlite

abstract class SunliteNativeFunction(val name: String, val arity: Int = 0) {

	override fun toString(): String {
		return "<native fn '${name}'>"
	}

	abstract fun call(vm: VM, args: Array<AnySunliteValue>): AnySunliteValue

}