package sunsetsatellite.vm.lox

class ChunkDebugInfo(
	val lines: IntArray,
	val file: String?,
	val name: String = "<script>",
)
class MutableChunkDebugInfo(val lines: MutableList<Int> = mutableListOf(), var file: String? = null, var name: String = "<script>") {
	fun toImmutable(): ChunkDebugInfo {
		return ChunkDebugInfo(lines.toIntArray(), file, name)
	}
}

class Chunk(val code: ByteArray, val constants: Array<AnyLoxValue>, val debugInfo: ChunkDebugInfo) {

	fun size(): Int {
		return code.size
	}

	override fun toString(): String {
		val sb = StringBuilder()
		sb.append("==== ${debugInfo.file.toString()}::${debugInfo.name} ====\n")
		for ((index, byte) in code.withIndex()) {
			sb.append(String.format("%04X: %02X\n", index, byte))
		}
		sb.append("=====${"=".repeat(debugInfo.file?.length?.plus(debugInfo.name.length) ?: 0)}=====\n")
		return sb.toString()
	}
}

class MutableChunk(val code: MutableList<Byte> = mutableListOf(), val constants: MutableList<AnyLoxValue> = mutableListOf(), val debugInfo: MutableChunkDebugInfo = MutableChunkDebugInfo()) {

	fun size(): Int {
		return code.size
	}

	fun toImmutable(): Chunk {
		return Chunk(code.toByteArray(), constants.toTypedArray(), debugInfo.toImmutable())
	}
}