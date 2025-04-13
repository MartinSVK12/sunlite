package sunsetsatellite.vm.sunlite

import sunsetsatellite.lang.sunlite.Sunlite
import sunsetsatellite.lang.sunlite.Sunlite.Companion.stacktrace
import java.util.*

class VM(val sunlite: Sunlite): Runnable {

	companion object {
		const val MAX_FRAMES: Int = 255

		val globals: MutableMap<String, AnyLoxValue> = HashMap()
		val openUpvalues: MutableList<SunliteUpvalue> = mutableListOf()

		fun arrayOfNils(size: Int): Array<AnyLoxValue> {
			return Array(size) { LoxNil }
		}
	}

	init {
		defineNative(object : SunliteNativeFunction("clock",0) {
			override fun call(vm: VM, args: Array<AnyLoxValue>): AnyLoxValue {
				return LoxNumber(System.currentTimeMillis().toDouble() / 1000)
			}
		})

		defineNative(object : SunliteNativeFunction("print",1) {
			override fun call(vm: VM, args: Array<AnyLoxValue>): AnyLoxValue {
				val value = args[0]
				println(if(value is LoxString) value.value else value.toString())
				return LoxNil
			}
		})

		defineNative(object : SunliteNativeFunction("str",1) {
			override fun call(vm: VM, args: Array<AnyLoxValue>): AnyLoxValue {
				return LoxString(args[0].toString())
			}
		})
	}

	val frameStack: Stack<CallFrame> = Stack()

	fun defineNative(function: SunliteNativeFunction){
		globals[function.name] = LoxNativeFuncObj(function)
	}

	override fun run(){
		var fr: CallFrame = frameStack.peek()

		while (fr.pc < fr.closure.function.chunk.code.size) {

			if(Sunlite.bytecodeDebug){
				val sb = StringBuilder()
				sb.append("STACK @ ${fr.closure.function.chunk.debugInfo.file}::${fr.closure.function.chunk.debugInfo.name}: ")
				for (value in fr.stack) {
					sb.append("[ ")
					sb.append(value)
					sb.append(" ]")
				}
				sb.append("\n")
				Disassembler.disassembleInstruction(sb, fr.closure.function.chunk, fr.pc)
				sunlite.printInfo(sb.toString())
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
					for (i in 0 until (frame.closure.function.arity + 1)) {
						fr.pop()
					}
					fr.push(value)
				}
				Opcodes.CONSTANT -> fr.push(readConstant(fr))
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
					val constant = readConstant(fr) as LoxString
					globals[constant.value] = fr.pop()
				}
				Opcodes.SET_GLOBAL -> {
					val constant = readConstant(fr) as LoxString
					if(!globals.containsKey(constant.value)) {
						runtimeError("Undefined variable '${constant.value}'.")
						return
					}
					globals[constant.value] = fr.peek()
				}
				Opcodes.GET_GLOBAL -> {
					val constant = readConstant(fr) as LoxString
					if(!globals.containsKey(constant.value)) {
						runtimeError("Undefined variable '${constant.value}'.")
						return
					}
					fr.push(globals[constant.value]!!)
				}
				Opcodes.SET_LOCAL -> fr.stack[readShort(fr)] = fr.peek()
				Opcodes.GET_LOCAL -> fr.push(fr.stack[readShort(fr)])
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
				Opcodes.CLOSURE -> {
					val constant = readConstant(fr) as LoxFuncObj
					val closure = SunliteClosure(constant.value)
					fr.push(LoxClosureObj(closure))
					for (i in 0 until closure.upvalues.size) {
						val isLocal: Int = readByte(fr)
						val index: Int = readShort(fr)
						if(isLocal == 1){
							closure.upvalues[i] = captureUpvalue(fr,index)
						} else {
							closure.upvalues[i] = fr.closure.upvalues[index];
						}
					}
				}
				Opcodes.GET_UPVALUE -> {
					val slot = readShort(fr)
					fr.push(fr.closure.upvalues[slot]?.closedValue ?: LoxNil)
				}
				Opcodes.SET_UPVALUE -> {
					val slot = readShort(fr)
					fr.closure.upvalues[slot]?.closedValue = fr.peek(0);
				}
				Opcodes.CLASS -> {
					val constant = readConstant(fr) as LoxString
					fr.push(LoxClassObj(SunliteClass(constant.value)))
				}

				Opcodes.SET_PROP -> TODO()
				Opcodes.GET_PROP -> TODO()
			}
		}
	}

	private fun readConstant(fr: CallFrame) = fr.closure.function.chunk.constants[readShort(fr)]

	private fun captureUpvalue(fr: CallFrame, index: Int): SunliteUpvalue {
		val value = fr.stack.elementAt(index)
		val found = openUpvalues.find { it.closedValue == value }

		if(found != null){
			return found
		}

		val upvalue = SunliteUpvalue(value)
		openUpvalues.add(upvalue)
		return upvalue
	}

	private fun callValue(callee: LoxValue<*>, argCount: Int): Boolean {
		if(callee.isObj()){
			when(callee.value) {
				is SunliteClosure -> {
					return call(callee as LoxClosureObj, argCount)
				}
				is SunliteNativeFunction -> {
					return callNative(callee as LoxNativeFuncObj, argCount)
				}
				is SunliteClass -> {
					val stack = frameStack.peek().stack
					stack.set(stack.size - argCount - 1, LoxClassInstanceObj(SunliteClassInstance(callee.value)))
					return true
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

	fun call(callee: LoxClosureObj, argCount: Int): Boolean {
		if(argCount != callee.value.function.arity){
			runtimeError("Expected ${callee.value.function.arity} arguments but got ${argCount}.")
			return false
		}

		if(frameStack.size == MAX_FRAMES){
			runtimeError("Stack overflow.");
			return false
		}

		val frame = CallFrame(callee.value)
		frame.stack.addAll(Array(argCount) { i -> frameStack.peek().peek(i) })

		frameStack.push(frame)
		return true
	}

	private fun isFalse(value: AnyLoxValue): Boolean {
		return value is LoxNil || value is LoxBool && !value.value
	}

	private fun readByte(frame: CallFrame): Int {
		return frame.closure.function.chunk.code[frame.pc++].toInt()
	}

	private fun readShort(frame: CallFrame): Int {
		frame.pc += 2
		val upperByte = frame.closure.function.chunk.code[frame.pc - 2]
		val lowerByte = frame.closure.function.chunk.code[frame.pc - 1]
		return (upperByte.toInt() shl 8 or lowerByte.toInt())
	}

	fun runtimeError(message: String) {

		val fr = frameStack.peek()

		sunlite.printErr(message)

		for (frame in frameStack) {
			sunlite.printErr("[line ${frame.closure.function.chunk.debugInfo.lines[frame.pc]}] in ${if(frame.closure.function.name == "") "script" else "${frame.closure.function.name}()"}")
		}

		val sb = StringBuilder()
		sb.append("stack @ ${fr.closure.function.chunk.debugInfo.file}::${fr.closure.function.chunk.debugInfo.name}: ")
		for (value in fr.stack) {
			sb.append("[ ")
			sb.append(value)
			sb.append(" ]")
		}
		sb.append("\n")
		sb.append("-> ")
		var offset = Disassembler.disassembleInstruction(sb, fr.closure.function.chunk, fr.pc)
		sunlite.printErr(sb.toString())

		frameStack.clear()

		if(stacktrace) {
			Exception("sunlite internal error").printStackTrace()
		}

		sunlite.hadRuntimeError = true
	}
}