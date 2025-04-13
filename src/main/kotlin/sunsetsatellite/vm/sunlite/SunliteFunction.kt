package sunsetsatellite.vm.sunlite

class SunliteFunction(val name: String, val chunk: Chunk, val arity: Int = 0, val upvalueCount: Int) {

	override fun toString(): String {
		return "<fn '${name}'>"
	}
}