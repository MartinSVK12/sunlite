package sunsetsatellite.vm.lox

import sunsetsatellite.lang.lox.Lox
import sunsetsatellite.lang.lox.Lox.Companion.stacktrace
import java.util.*

class VM(val lox: Lox): Runnable {

	companion object {
		const val MAX_FRAMES: Int = 255

		val globals: MutableMap<String, AnyLoxValue> = HashMap()

		fun arrayOfNils(size: Int): Array<AnyLoxValue> {
			return Array(size) { LoxNil }
		}
	}

	init {
		defineNative(object : LoxNativeFunction("clock",0) {
			override fun call(vm: VM, args: Array<AnyLoxValue>): AnyLoxValue {
				return LoxNumber(System.currentTimeMillis().toDouble() / 1000)
			}
		})

		defineNative(object : LoxNativeFunction("print",1) {
			override fun call(vm: VM, args: Array<AnyLoxValue>): AnyLoxValue {
				val value = args[0]
				println(if(value is LoxString) value.value else value.toString())
				return LoxNil
			}
		})

		defineNative(object : LoxNativeFunction("str",1) {
			override fun call(vm: VM, args: Array<AnyLoxValue>): AnyLoxValue {
				return LoxString(args[0].toString())
			}
		})
	}

	val frameStack: Stack<CallFrame> = Stack()

	fun defineNative(function: LoxNativeFunction){
		globals[function.name] = LoxNativeFuncObj(function)
	}

	override fun run(){
		var fr: CallFrame = frameStack.peek()

		while (fr.pc < fr.function.chunk.code.size) {

			if(Lox.bytecodeDebug){
				val sb = StringBuilder()
				sb.append("STACK @ ${fr.function.chunk.debugInfo.file}::${fr.function.chunk.debugInfo.name}: ")
				for (value in fr.stack) {
					sb.append("[ ")
					sb.append(value)
					sb.append(" ]")
				}
				sb.append("\n")
				Disassembler.disassembleInstruction(sb, fr.function.chunk, fr.pc)
				lox.printInfo(sb.toString())
			}

			val instruction = readByte(fr);
			when (Opcodes.entries[instruction]) {
				Opcodes.NOP -> {
					return
				}
				Opcodes.RETURN -> {
					val value: AnyLoxValue = fr.pop()
					if(frameStack.size == 1){
						return
					}
					val frame = frameStack.pop()
					fr = frameStack.peek()
					for (i in 0 until (frame.function.arity + 1)) {
						fr.pop()
					}
					fr.push(value)
				}
				Opcodes.CONSTANT -> fr.push(fr.function.chunk.constants[readShort(fr)])
				Opcodes.NEGATE -> {
					if(fr.peek() !is LoxNumber){
						runtimeError("Operand must be a number.")
						return
					}
					fr.push(-(fr.pop() as LoxNumber))
				}
				Opcodes.ADD -> {
					if(fr.peek() is LoxString && fr.peek(1) is LoxString){
						val right = fr.pop() as LoxString
						val left = fr.pop() as LoxString
						fr.push(left + right)
					} else if(fr.peek() is LoxNumber && fr.peek(1) is LoxNumber){
						val right = fr.pop() as LoxNumber
						val left = fr.pop() as LoxNumber
						fr.push(left + right)
					} else {
						runtimeError("Operands must be numbers or strings.")
						return
					}
				}
				Opcodes.SUB -> {
					if(fr.peek() !is LoxNumber || fr.peek(1) !is LoxNumber){
						runtimeError("Operands must be a number.")
						return
					}
					val right = fr.pop() as LoxNumber
					val left = fr.pop() as LoxNumber
					fr.push(left - right)
				}
				Opcodes.MULTIPLY -> {
					if(fr.peek() !is LoxNumber || fr.peek(1) !is LoxNumber){
						runtimeError("Operands must be a number.")
						return
					}
					val right = fr.pop() as LoxNumber
					val left = fr.pop() as LoxNumber
					fr.push(left * right)
				}
				Opcodes.DIVIDE -> {
					if(fr.peek() !is LoxNumber || fr.peek(1) !is LoxNumber){
						runtimeError("Operands must be a number.")
						return
					}
					val right = fr.pop() as LoxNumber
					val left = fr.pop() as LoxNumber
					fr.push(left / right)
				}
				Opcodes.NIL -> fr.push(LoxNil)
				Opcodes.TRUE -> fr.push(LoxBool(true))
				Opcodes.FALSE -> fr.push(LoxBool(false))
				Opcodes.NOT -> fr.push(LoxBool(isFalse(fr.pop())))
				Opcodes.EQUAL -> fr.push(LoxBool(fr.pop() == fr.pop()))
				Opcodes.GREATER -> {
					if(fr.peek() !is LoxNumber || fr.peek(1) !is LoxNumber){
						runtimeError("Operands must be a number.")
						return
					}
					fr.push(LoxBool(fr.pop() as LoxNumber > fr.pop() as LoxNumber))
				}
				Opcodes.LESS -> {
					if(fr.peek() !is LoxNumber || fr.peek(1) !is LoxNumber){
						runtimeError("Operands must be a number.")
						return
					}
					fr.push(LoxBool((fr.pop() as LoxNumber) < (fr.pop() as LoxNumber)))
				}
				Opcodes.PRINT -> println(fr.pop())
				Opcodes.POP -> fr.pop()
				Opcodes.DEF_GLOBAL -> {
					val constant = fr.function.chunk.constants[readShort(fr)] as LoxString
					globals[constant.value] = fr.pop()
				}
				Opcodes.SET_GLOBAL -> {
					val constant = fr.function.chunk.constants[readShort(fr)] as LoxString
					if(!globals.containsKey(constant.value)) {
						runtimeError("Undefined variable '${constant}'.")
						return
					}
					globals[constant.value] = fr.peek()
				}
				Opcodes.GET_GLOBAL -> {
					val constant = fr.function.chunk.constants[readShort(fr)] as LoxString
					if(!globals.containsKey(constant.value)) {
						runtimeError("Undefined variable '${constant}'.")
						return
					}
					fr.push(globals[constant.value]!!)
				}
				Opcodes.SET_LOCAL -> fr.locals[readShort(fr)] = fr.peek()
				Opcodes.GET_LOCAL -> fr.push(fr.locals[readShort(fr)])
				Opcodes.JUMP_IF_FALSE -> {
					val short = readShort(fr)
					if(isFalse(fr.peek())) {
						fr.pc += short
					}
				}
				Opcodes.JUMP -> {
					val short = readShort(fr)
					fr.pc += short
				}
				Opcodes.LOOP -> {
					val short = readShort(fr)
					fr.pc -= short
				}
				Opcodes.CALL -> {
					val argCount: Int = readByte(fr)
					if(!callValue(fr.peek(argCount), argCount)){
						return
					}
					fr = frameStack.peek()
				}
			}
		}
	}

	private fun callValue(callee: LoxValue<*>, argCount: Int): Boolean {
		if(callee.isObj()){
			when(callee.value) {
				is LoxFunction -> {
					return call(callee as LoxFuncObj, argCount)
				}
				is LoxNativeFunction -> {
					return callNative(callee as LoxNativeFuncObj, argCount)
				}
			}
		}
		runtimeError("Can only call functions.")
		return false
	}

	fun callNative(callee: LoxNativeFuncObj, argCount: Int): Boolean {
		if(argCount != callee.value.arity){
			runtimeError("Expected ${callee.value.arity} arguments but got ${argCount}.")
			return false
		}

		frameStack.peek().stack.removeAt((frameStack.peek().stack.size-1)-argCount)
		frameStack.peek().push(callee.value.call(this, Array(argCount) { i -> frameStack.peek().pop() }))
		return true
	}

	fun call(callee: LoxFuncObj, argCount: Int): Boolean {
		if(argCount != callee.value.arity){
			runtimeError("Expected ${callee.value.arity} arguments but got ${argCount}.")
			return false
		}

		if(frameStack.size == MAX_FRAMES){
			runtimeError("Stack overflow.");
			return false
		}

		val frame = CallFrame(callee.value, Array(argCount) { i -> frameStack.peek().peek(i) })
		frameStack.push(frame)
		return true
	}

	private fun isFalse(value: AnyLoxValue): Boolean {
		return value is LoxNil || value is LoxBool && !value.value
	}

	private fun readByte(frame: CallFrame): Int {
		return frame.function.chunk.code[frame.pc++].toInt()
	}

	private fun readShort(frame: CallFrame): Int {
		frame.pc += 2
		val upperByte = frame.function.chunk.code[frame.pc - 2]
		val lowerByte = frame.function.chunk.code[frame.pc - 1]
		return (upperByte.toInt() shl 8 or lowerByte.toInt())
	}

	fun runtimeError(message: String) {

		val fr = frameStack.peek()

		lox.printErr(message)

		for (frame in frameStack) {
			lox.printErr("[line ${frame.function.chunk.debugInfo.lines[frame.pc]}] in ${if(frame.function.name == "") "script" else "${frame.function.name}()"}")
		}

		val sb = StringBuilder()
		sb.append("STACK @ ${fr.function.chunk.debugInfo.file}::${fr.function.chunk.debugInfo.name}: ")
		for (value in fr.stack) {
			sb.append("[ ")
			sb.append(value)
			sb.append(" ]")
		}
		sb.append("\n")
		Disassembler.disassembleInstruction(sb, fr.function.chunk, fr.pc)
		lox.printErr(sb.toString())

		frameStack.clear()

		if(stacktrace) {
			Exception("lox internal stack trace").printStackTrace()
		}

		lox.hadRuntimeError = true
	}
}