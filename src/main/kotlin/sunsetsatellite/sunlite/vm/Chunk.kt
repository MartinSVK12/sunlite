package sunsetsatellite.sunlite.vm

import sunsetsatellite.sunlite.lang.ClassModifier
import java.io.DataInputStream
import java.io.DataOutputStream

class ChunkDebugInfo(
    val lines: IntArray,
    val file: String?,
    val name: String = "<script>",
    val lineData: Map<Int, String?> = mapOf(),
    val locals: List<String> = listOf(),
    val classData: Map<String, ClassData> = mapOf()
) {

    companion object {
        fun read(s: DataInputStream): ChunkDebugInfo{
            val linesSize = s.readInt()
            val lines = IntArray(linesSize) { s.readInt() }
            val file = s.readUTF()
            val name = s.readUTF()
            val lineDataSize = s.readInt()
            val lineData = mutableMapOf<Int, String?>()
            repeat(lineDataSize) {
                lineData[s.readInt()] = s.readUTF()
            }
            val localsSize = s.readInt()
            val locals = mutableListOf<String>()
            repeat(localsSize) {
                locals.add(s.readUTF())
            }
            val classData: MutableMap<String, ClassData> = mutableMapOf()
            val classDataSize = s.readInt()
            repeat(classDataSize) {
                val name = s.readUTF()
                classData[name] = ClassData.read(s)
            }
            return ChunkDebugInfo(lines, file, name, lineData.toMap(), locals, classData.toMap())
        }
    }

    fun write(s: DataOutputStream){
        s.writeInt(lines.size)
        lines.forEach { s.writeInt(it) }
        s.writeUTF(file ?: "<unknown>")
        s.writeUTF(name)
        s.writeInt(lineData.size)
        lineData.forEach { line, data ->
            s.writeInt(line)
            s.writeUTF(data ?: "<unknown>")
        }
        s.writeInt(locals.size)
        locals.forEach { s.writeUTF(it) }
        s.writeInt(classData.size)
        classData.forEach { (name, data) ->
            s.writeUTF(name)
            data.write(s)
        }
    }
}

class MutableChunkDebugInfo(
    val lines: MutableList<Int> = mutableListOf(),
    var file: String? = null,
    var name: String = "<script>",
    val lineData: MutableMap<Int, String?> = mutableMapOf(),
    val locals: MutableList<String> = mutableListOf(),
    var classData: MutableMap<String, MutableClassData> = mutableMapOf()
) {
    fun toImmutable(): ChunkDebugInfo {
        return ChunkDebugInfo(lines.toIntArray(), file, name, lineData, locals, classData.mapValues { it.value.toImmutable() }.toMap())
    }
}

class ClassData(
    val name: String,
    val superclass: String,
    val superinterfaces: List<String>,
    val typeParameters: List<String>,
    val modifier: ClassModifier
) {
    companion object {
        fun read(s: DataInputStream): ClassData {
            val name = s.readUTF()
            val superclass = s.readUTF()
            val superinterfaceCount = s.readInt()
            val superinterfaces = mutableListOf<String>()
            repeat(superinterfaceCount) { superinterfaces.add(s.readUTF()) }
            val typeParameterCount = s.readInt()
            val typeParameters = mutableListOf<String>()
            repeat(typeParameterCount) { typeParameters.add(s.readUTF()) }
            val modifierOrdinal = s.readInt()
            return ClassData(name, superclass, superinterfaces, typeParameters, ClassModifier.entries[modifierOrdinal])
        }
    }

    fun write(s: DataOutputStream) {
        s.writeUTF(name)
        s.writeUTF(superclass)
        s.writeInt(superinterfaces.size)
        superinterfaces.forEach { s.writeUTF(it) }
        s.writeInt(typeParameters.size)
        typeParameters.forEach { s.writeUTF(it) }
        s.writeInt(modifier.ordinal)
    }
}

class MutableClassData(
    var name: String,
    var superclass: String,
    val superinterfaces: MutableList<String>,
    val typeParameters: MutableList<String>,
    var modifier: ClassModifier
) {
    fun toImmutable(): ClassData {
        return ClassData(name, superclass, superinterfaces.toList(), typeParameters.toList(), modifier)
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
            val constants = Array<AnySLValue>(constantsSize) { SLNil }

            for (i in 0 until constantsSize) {
                constants[i] = AnySLValue.read(s)
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
            sb.append(String.format("%04d: %02X\n", index, byte))
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
            sb.append(String.format("%04d: %02X\n", index, byte))
        }
        sb.append("=====${"=".repeat(debugInfo.file?.length?.plus(debugInfo.name.length) ?: 0)}=====\n")
        return sb.toString()
    }
}