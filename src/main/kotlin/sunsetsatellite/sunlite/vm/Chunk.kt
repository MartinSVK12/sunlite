package sunsetsatellite.sunlite.vm

import java.io.DataInputStream
import java.io.DataOutputStream

class ChunkDebugInfo(
    val lines: IntArray,
    val file: String?,
    val name: String = "<script>",
    val originalFile: Map<Int, String?> = mapOf(),
) {

    companion object {
        fun read(s: DataInputStream): ChunkDebugInfo{
            val linesSize = s.readInt()
            val lines = IntArray(linesSize) { s.readInt() }
            val file = s.readUTF()
            val name = s.readUTF()
            return ChunkDebugInfo(lines, file, name)
        }
    }

    fun write(s: DataOutputStream){
        s.writeInt(lines.size)
        lines.forEach { s.writeInt(it) }
        s.writeUTF(file ?: "<unknown>")
        s.writeUTF(name)
    }
}

class MutableChunkDebugInfo(
    val lines: MutableList<Int> = mutableListOf(),
    var file: String? = null,
    var name: String = "<script>",
    val originalFile: MutableMap<Int, String?> = mutableMapOf(),
) {
    fun toImmutable(): ChunkDebugInfo {
        return ChunkDebugInfo(lines.toIntArray(), file, name, originalFile)
    }
}

class Chunk(
    val code: ByteArray,
    val exceptions: Map<IntRange, IntRange>,
    val constants: Array<AnySLValue>,
    val debugInfo: ChunkDebugInfo
) {

    companion object {
        fun read(s: DataInputStream): Chunk {
            val codeSize = s.readInt()
            val code = ByteArray(codeSize)
            s.readFully(code)
            val exceptionsSize = s.readInt()
            val exceptions = mutableMapOf<IntRange, IntRange>()
            for (i in 0 until exceptionsSize) {
                val protectedFirst = s.readInt()
                val protectedLast = s.readInt()
                val handlerFirst = s.readInt()
                val handlerLast = s.readInt()
                exceptions[protectedFirst..protectedLast] = handlerFirst..handlerLast
            }
            val constantsSize = s.readInt()
            val constants = Array(constantsSize) {
                AnySLValue.read(s)
            }

            return Chunk(code, exceptions.toMap(), constants, ChunkDebugInfo.read(s))
        }
    }

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

    fun write(s: DataOutputStream){
        s.writeInt(size())
        s.write(code)
        s.writeInt(exceptions.size)
        exceptions.forEach { (protected, handler) ->
            s.writeInt(protected.first)
            s.writeInt(protected.last)
            s.writeInt(handler.first)
            s.writeInt(handler.last)
        }
        s.writeInt(constants.size)
        constants.forEach { it.write(s) }
        debugInfo.write(s)
    }
}

class MutableChunk(
    val code: MutableList<Byte> = mutableListOf(),
    val exceptions: MutableMap<IntRange, IntRange> = mutableMapOf(),
    val constants: MutableList<AnySLValue> = mutableListOf(),
    val debugInfo: MutableChunkDebugInfo = MutableChunkDebugInfo()
) {

    fun size(): Int {
        return code.size
    }

    fun toImmutable(): Chunk {
        return Chunk(code.toByteArray(), exceptions.toMap(), constants.toTypedArray(), debugInfo.toImmutable())
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("==== ${debugInfo.file.toString()}::${debugInfo.name} (mutable) ====\n")
        for ((index, byte) in code.withIndex()) {
            sb.append(String.format("%04X: %02X\n", index, byte))
        }
        sb.append("=====${"=".repeat(debugInfo.file?.length?.plus(debugInfo.name.length) ?: 0)}=====\n")
        return sb.toString()
    }
}