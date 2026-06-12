package sunsetsatellite.sunlite.vm

class SLBoundMethod(val method: SLClosure, val receiver: AnySLValue) {

    override fun toString(): String {
        return "<method '${receiver}::$method'>"
    }

    fun copy(): SLBoundMethod {
        return SLBoundMethod(method.copy(), receiver.copy())
    }
}