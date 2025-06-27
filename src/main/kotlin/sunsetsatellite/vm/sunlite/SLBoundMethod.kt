package sunsetsatellite.vm.sunlite

class SLBoundMethod(val method: SLClosure, val receiver: AnySLValue) {

	override fun toString(): String {
		return method.toString()
	}

	fun copy(): SLBoundMethod {
		return SLBoundMethod(method.copy(), receiver.copy())
	}
}