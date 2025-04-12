package sunsetsatellite.vm.lox

object Disassembler {
	fun disassembleChunk(chunk: Chunk): String {
		val sb = StringBuilder()
		sb.append("==== ${chunk.debugInfo.file.toString()}::${chunk.debugInfo.name} ====\n")

		var offset = 0
		while (offset < chunk.size()) {
			offset = disassembleInstruction(sb, chunk, offset)
		}

		sb.append("=====${"=".repeat(chunk.debugInfo.file?.length?.plus(chunk.debugInfo.name.length) ?: 0)}=====\n")
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
			Opcodes.NOP -> return simpleInstruction(sb, opcode.name, offset);
			Opcodes.RETURN -> return simpleInstruction(sb, opcode.name, offset);
			Opcodes.CONSTANT -> return constantInstruction(sb, opcode.name, chunk, offset )
			Opcodes.NEGATE -> return simpleInstruction(sb, opcode.name, offset);
			Opcodes.ADD -> return simpleInstruction(sb, opcode.name, offset);
			Opcodes.SUB -> return simpleInstruction(sb, opcode.name, offset);
			Opcodes.MULTIPLY -> return simpleInstruction(sb, opcode.name, offset);
			Opcodes.DIVIDE -> return simpleInstruction(sb, opcode.name, offset);
			Opcodes.NIL -> return simpleInstruction(sb, opcode.name, offset);
			Opcodes.TRUE -> return simpleInstruction(sb, opcode.name, offset);
			Opcodes.FALSE -> return simpleInstruction(sb, opcode.name, offset);
			Opcodes.NOT -> return simpleInstruction(sb, opcode.name, offset);
			Opcodes.EQUAL -> return simpleInstruction(sb, opcode.name, offset);
			Opcodes.GREATER -> return simpleInstruction(sb, opcode.name, offset);
			Opcodes.LESS -> return simpleInstruction(sb, opcode.name, offset);
			Opcodes.PRINT -> return simpleInstruction(sb, opcode.name, offset);
			Opcodes.POP -> return simpleInstruction(sb, opcode.name, offset);
			Opcodes.DEF_GLOBAL -> return constantInstruction(sb, opcode.name, chunk, offset )
			Opcodes.SET_GLOBAL -> return constantInstruction(sb, opcode.name, chunk, offset )
			Opcodes.GET_GLOBAL -> return constantInstruction(sb, opcode.name, chunk, offset )
			Opcodes.SET_LOCAL -> return shortInstruction(sb, opcode.name, chunk, offset )
			Opcodes.GET_LOCAL -> return shortInstruction(sb, opcode.name, chunk, offset )
			Opcodes.JUMP_IF_FALSE -> return jumpInstruction(sb, opcode.name, 1, chunk, offset )
			Opcodes.JUMP -> return jumpInstruction(sb, opcode.name, 1, chunk, offset )
			Opcodes.LOOP -> return jumpInstruction(sb, opcode.name, -1, chunk, offset )
			Opcodes.CALL -> return byteInstruction(sb, opcode.name, chunk, offset )
			else -> return offset + 1
		}
	}

	private fun jumpInstruction(sb: StringBuilder, name: String, sign: Int, chunk: Chunk, offset: Int): Int {
		val jmp = (chunk.code[offset + 1].toInt() shl 8) or chunk.code[offset + 2].toInt()
		sb.append(String.format("%-16s %4d -> %d\n", name, offset, offset + 3 + sign * jmp))
		return offset + 3
	}

	private fun byteInstruction(sb: StringBuilder, name: String, chunk: Chunk, offset: Int): Int {
		val byte = chunk.code[offset + 1]
		sb.append(String.format("%-16s %4d\n", name, byte))
		return offset + 2
	}

	private fun shortInstruction(sb: StringBuilder, name: String, chunk: Chunk, offset: Int): Int {
		val short = (chunk.code[offset + 1].toInt() shl 8) or chunk.code[offset + 2].toInt()
		sb.append(String.format("%-16s %4d\n", name, short))
		return offset + 3
	}

	private fun constantInstruction(sb: StringBuilder, name: String, chunk: Chunk, offset: Int): Int {
		val constant = (chunk.code[offset + 1].toInt() shl 8) or chunk.code[offset + 2].toInt()
		sb.append(String.format("%-16s %4d '", name, constant))
		if(constant > chunk.constants.size){
			sb.append("<error reading constant>")
		} else {
			sb.append(chunk.constants[constant].toString())
		}
		sb.append("'\n")
		return offset + 3
	}

	private fun simpleInstruction(sb: StringBuilder, name: String, offset: Int): Int {
		sb.append(String.format("%s\n", name))
		return offset + 1
	}


}