package sunsetsatellite.lang.sunlite

import sunsetsatellite.vm.sunlite.Chunk
import sunsetsatellite.vm.sunlite.Opcodes
import sunsetsatellite.vm.sunlite.SLFuncObj

object Disassembler {
	fun disassembleChunk(chunk: Chunk): String {
		val sb = StringBuilder()
		sb.append("==== ${chunk.debugInfo.file.toString()}::${chunk.debugInfo.name} ====\n")

		var offset = 0
		while (offset < chunk.size()) {
			offset = disassembleInstruction(sb, chunk, offset)
		}
		sb.append("==== Exception Table ====\n")

		for ((index, exception) in chunk.exceptions.entries.withIndex()) {
			sb.append(exception.key.toString() + " -> " + exception.value.toString() + "\n")
		}

		sb.append("==== ${chunk.debugInfo.file.toString()}::${chunk.debugInfo.name} ====\n")
		//sb.append("=====${"=".repeat(chunk.debugInfo.file?.length?.plus(chunk.debugInfo.name.length) ?: 0)}=====\n")
		return sb.toString()
	}

	fun disassembleInstruction(sb: StringBuilder, chunk: Chunk, offset: Int): Int {
		sb.append(String.format("%04d ",offset))
		if(offset > 0 && chunk.debugInfo.lines[offset] == chunk.debugInfo.lines[offset - 1]){
			sb.append("   | ");
		} else {
			sb.append(String.format("%4d ",chunk.debugInfo.lines[offset]));
		}
		val inst = chunk.code[offset]
		val opcode: Opcodes = Opcodes.entries[inst.toInt()]
		when (opcode) {
			Opcodes.NOP -> return simpleInstruction(sb, opcode.name, offset)
			Opcodes.RETURN -> return simpleInstruction(sb, opcode.name, offset)
			Opcodes.CONSTANT -> return constantInstruction(sb, opcode.name, chunk, offset)
			Opcodes.NEGATE -> return simpleInstruction(sb, opcode.name, offset)
			Opcodes.ADD -> return simpleInstruction(sb, opcode.name, offset)
			Opcodes.SUB -> return simpleInstruction(sb, opcode.name, offset)
			Opcodes.MULTIPLY -> return simpleInstruction(sb, opcode.name, offset)
			Opcodes.DIVIDE -> return simpleInstruction(sb, opcode.name, offset)
			Opcodes.NIL -> return simpleInstruction(sb, opcode.name, offset)
			Opcodes.TRUE -> return simpleInstruction(sb, opcode.name, offset)
			Opcodes.FALSE -> return simpleInstruction(sb, opcode.name, offset)
			Opcodes.NOT -> return simpleInstruction(sb, opcode.name, offset)
			Opcodes.EQUAL -> return simpleInstruction(sb, opcode.name, offset)
			Opcodes.GREATER -> return simpleInstruction(sb, opcode.name, offset)
			Opcodes.LESS -> return simpleInstruction(sb, opcode.name, offset)
			Opcodes.PRINT -> return simpleInstruction(sb, opcode.name, offset)
			Opcodes.POP -> return simpleInstruction(sb, opcode.name, offset)
			Opcodes.DEF_GLOBAL -> return constantInstruction(sb, opcode.name, chunk, offset)
			Opcodes.SET_GLOBAL -> return constantInstruction(sb, opcode.name, chunk, offset)
			Opcodes.GET_GLOBAL -> return constantInstruction(sb, opcode.name, chunk, offset)
			Opcodes.SET_LOCAL -> return shortInstruction(sb, opcode.name, chunk, offset)
			Opcodes.GET_LOCAL -> return shortInstruction(sb, opcode.name, chunk, offset)
			Opcodes.JUMP_IF_FALSE -> return jumpInstruction(sb, opcode.name, 1, chunk, offset)
			Opcodes.JUMP -> return jumpInstruction(sb, opcode.name, 1, chunk, offset)
			Opcodes.LOOP -> return jumpInstruction(sb, opcode.name, -1, chunk, offset)
			Opcodes.CALL -> return twoByteInstruction(sb, opcode.name, chunk, offset)
			Opcodes.CLOSURE -> return closureInstruction(sb, opcode.name, chunk, offset)
			Opcodes.GET_UPVALUE -> return shortInstruction(sb, opcode.name, chunk, offset)
			Opcodes.SET_UPVALUE -> return shortInstruction(sb, opcode.name, chunk, offset)
			Opcodes.CLASS -> return constantInstruction(sb, opcode.name, chunk, offset)
			Opcodes.SET_PROP -> return constantInstruction(sb, opcode.name, chunk, offset)
			Opcodes.GET_PROP -> return constantInstruction(sb, opcode.name, chunk, offset)
			Opcodes.METHOD -> return constantInstruction(sb, opcode.name, chunk, offset)
			Opcodes.FIELD -> return constantInstruction(sb, opcode.name, chunk, offset)
			Opcodes.STATIC_FIELD -> return constantInstruction(sb, opcode.name, chunk, offset)
			Opcodes.INHERIT -> return simpleInstruction(sb, opcode.name, offset)
			Opcodes.GET_SUPER -> return constantInstruction(sb, opcode.name, chunk, offset)
			Opcodes.ARRAY_GET -> return simpleInstruction(sb, opcode.name, offset)
			Opcodes.ARRAY_SET -> return simpleInstruction(sb, opcode.name, offset)
			Opcodes.THROW -> return simpleInstruction(sb, opcode.name, offset)
            Opcodes.CHECK -> return constantInstruction(sb, opcode.name, chunk, offset)
			Opcodes.TYPE_PARAM -> return constantInstruction(sb, opcode.name, chunk, offset)
        }
	}

	private fun closureInstruction(sb: StringBuilder, name: String, chunk: Chunk, startOffset: Int): Int {
		var offset = startOffset
		val constant = (chunk.code[offset + 1].toInt() shl 8) or chunk.code[offset + 2].toInt()
		offset += 3
		sb.append(String.format("%-16s (%02X) %4d ", name, Opcodes.valueOf(name).ordinal, constant))
		if(constant > chunk.constants.size){
			sb.append("<error reading constant>")
		} else {
			sb.append(chunk.constants[constant].toString())
		}
		sb.append("\n")
		val function = chunk.constants[constant] as SLFuncObj
		for (i in 0 until function.value.upvalueCount) {
			val isLocal = chunk.code[offset++].toInt()
			val index = (chunk.code[offset].toInt() shl 8) or chunk.code[offset + 1].toInt()
			offset += 2
			sb.append(String.format("%04d      L  %s %d\n", offset - 3, if(isLocal == 1) "local" else "upvalue", index))
		}
		return offset
	}

	private fun jumpInstruction(sb: StringBuilder, name: String, sign: Int, chunk: Chunk, offset: Int): Int {
		val jmp = (chunk.code[offset + 1].toInt() shl 8) or chunk.code[offset + 2].toInt()
		sb.append(String.format("%-16s (%02X) %4d -> %d\n", name, Opcodes.valueOf(name).ordinal, offset, offset + 3 + sign * jmp))
		return offset + 3
	}

	private fun byteInstruction(sb: StringBuilder, name: String, chunk: Chunk, offset: Int): Int {
		val byte = chunk.code[offset + 1]
		sb.append(String.format("%-16s (%02X) %4d\n", name, Opcodes.valueOf(name).ordinal, byte))
		return offset + 2
	}

	private fun twoByteInstruction(sb: StringBuilder, name: String, chunk: Chunk, offset: Int): Int {
		val byte = chunk.code[offset + 1]
		val byte2 = chunk.code[offset + 2]
		sb.append(String.format("%-16s (%02X) %4d %4d\n", name, Opcodes.valueOf(name).ordinal, byte, byte2))
		return offset + 2
	}

	private fun shortInstruction(sb: StringBuilder, name: String, chunk: Chunk, offset: Int): Int {
		val short = (chunk.code[offset + 1].toInt() shl 8) or chunk.code[offset + 2].toInt()
		sb.append(String.format("%-16s (%02X) %4d\n", name, Opcodes.valueOf(name).ordinal, short))
		return offset + 3
	}

	private fun constantInstruction(sb: StringBuilder, name: String, chunk: Chunk, offset: Int): Int {
		val constant = (chunk.code[offset + 1].toInt() shl 8) or chunk.code[offset + 2].toInt()
		sb.append(String.format("%-16s (%02X) %4d ", name, Opcodes.valueOf(name).ordinal, constant))
		if(constant > chunk.constants.size){
			sb.append("<error reading constant>")
		} else {
			sb.append(chunk.constants[constant].toString())
		}
		sb.append("\n")
		return offset + 3
	}

	private fun simpleInstruction(sb: StringBuilder, name: String, offset: Int): Int {
		sb.append(String.format("%-16s (%02X)\n", name, Opcodes.valueOf(name).ordinal))
		return offset + 1
	}


}