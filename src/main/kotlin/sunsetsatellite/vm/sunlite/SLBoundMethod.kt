package sunsetsatellite.vm.sunlite

class SLBoundMethod(val method: SLClosure, val receiver: AnySunliteValue) {

	override fun toString(): String {
		return method.toString()
	}

}