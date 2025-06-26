package sunsetsatellite.vm.sunlite

class SunliteBoundMethod(val method: SunliteClosure, val receiver: AnySunliteValue) {

	override fun toString(): String {
		return method.toString()
	}

}