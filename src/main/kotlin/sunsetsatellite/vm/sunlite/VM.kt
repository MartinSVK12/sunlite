package sunsetsatellite.vm.sunlite

import sunsetsatellite.lang.sunlite.Disassembler
import sunsetsatellite.lang.sunlite.FunctionModifier
import sunsetsatellite.lang.sunlite.PrimitiveType
import sunsetsatellite.lang.sunlite.Sunlite
import sunsetsatellite.lang.sunlite.Sunlite.Companion.stacktrace
import sunsetsatellite.lang.sunlite.Type
import sunsetsatellite.vm.sunlite.VM.Companion.MAX_FRAMES
import sunsetsatellite.vm.sunlite.VM.Companion.globals
import sunsetsatellite.vm.sunlite.VM.Companion.openUpvalues
import java.time.Clock.tick
import java.util.*

class VM(val sunlite: Sunlite, val launchArgs: Array<String>): Runnable {

	var ignoreBreakpoints: Boolean = false
	var breakpointHit: Boolean = false
	var continueExecution: Boolean = false
	var lastBreakpointLine: Int = -1
	var overrideFunction: SLFunction? = null

	var currentException: AnySLValue? = null
	val exceptionStacktrace: Stack<CallFrame> = Stack()

	var currentFrame: CallFrame? = null

	companion object {
		const val MAX_FRAMES: Int = 255

		val globals: MutableMap<String, AnySLValue> = HashMap()
		val openUpvalues: MutableList<SLUpvalue> = mutableListOf()

		fun arrayOfNils(size: Int): Array<AnySLValue> {
			return Array(size) { SLNil }
		}
	}

	init {
		Natives.registerNatives(this)
	}

	val frameStack: Stack<CallFrame> = Stack()

	fun <E>Stack<E>.peek(i: Int): E{
		val len = this.size

		if (len == 0) throw EmptyStackException()
		return this.elementAt(len - i - 1)
	}


	fun defineNative(function: SLNativeFunction){
		globals[function.name] = SLNativeFuncObj(function)
	}

	fun isInitialized(): Boolean {
		return currentFrame != null
	}

	fun tick(){
		try {
			if(currentFrame == null) {
				runtimeError("VM uninitialized.")
				return
			}
			var fr: CallFrame = currentFrame!!
			if(fr.pc < fr.closure.function.chunk.code.size){
				if(Sunlite.bytecodeDebug){
					val sb = StringBuilder()
					sb.append("STACK @ ${fr.closure.function.chunk.debugInfo.file}::${fr.closure.function.chunk.debugInfo.name}: ")
					for (value in fr.stack) {
						sb.append("[ ")
						sb.append(value)
						sb.append(" ]")
					}
					if(fr.stack.isEmpty()){
						sb.append("[ ]")
					}
					sb.append("\n")
					sb.append("LOCALS @ ${fr.closure.function.chunk.debugInfo.file}::${fr.closure.function.chunk.debugInfo.name}: ")
					for (value in fr.locals) {
						sb.append("[ ")
						sb.append(value)
						sb.append(" ]")
					}
					if(fr.locals.isEmpty()){
						sb.append("[ ]")
					}
					sb.append("\n")
					Disassembler.disassembleInstruction(sb, fr.closure.function.chunk, fr.pc)
					sunlite.printInfo(sb.toString())
				}

				val instruction = readByte(fr);
				when (Opcodes.entries[instruction]) {
					Opcodes.NOP -> {

					}
					Opcodes.RETURN -> {
						val value: AnySLValue = fr.pop()
						if(frameStack.size == 1){
							return
						}
						val frame = frameStack.pop()
						fr = frameStack.peek()
						currentFrame = fr
						for (i in 0 until (frame.closure.function.arity + 1 + frame.closure.function.typeParams.size)) {
							fr.pop()
						}
						fr.push(value)
					}
					Opcodes.CONSTANT -> fr.push(readConstant(fr))
					Opcodes.NEGATE -> {
						if(fr.peek() !is SLNumber){
							runtimeError("Operand must be a number.")
							return
						}
						fr.push(-(fr.pop() as SLNumber))
					}
					Opcodes.ADD -> {
						if(fr.peek() is SLString && fr.peek(1) is SLString){
							val right = fr.pop() as SLString
							val left = fr.pop() as SLString
							fr.push(left + right)
						} else if(fr.peek() is SLNumber && fr.peek(1) is SLNumber){
							val right = fr.pop() as SLNumber
							val left = fr.pop() as SLNumber
							fr.push(left + right)
						} else {
							runtimeError("Operands must be numbers or strings.")
							return
						}
					}
					Opcodes.SUB -> {
						if(fr.peek() !is SLNumber || fr.peek(1) !is SLNumber){
							runtimeError("Operands must be a number.")
							return
						}
						val right = fr.pop() as SLNumber
						val left = fr.pop() as SLNumber
						fr.push(left - right)
					}
					Opcodes.MULTIPLY -> {
						if(fr.peek() !is SLNumber || fr.peek(1) !is SLNumber){
							runtimeError("Operands must be a number.")
							return
						}
						val right = fr.pop() as SLNumber
						val left = fr.pop() as SLNumber
						fr.push(left * right)
					}
					Opcodes.DIVIDE -> {
						if(fr.peek() !is SLNumber || fr.peek(1) !is SLNumber){
							runtimeError("Operands must be a number.")
							return
						}
						val right = fr.pop() as SLNumber
						val left = fr.pop() as SLNumber
						fr.push(left / right)
					}
					Opcodes.NIL -> fr.push(SLNil)
					Opcodes.TRUE -> fr.push(SLBool(true))
					Opcodes.FALSE -> fr.push(SLBool(false))
					Opcodes.NOT -> fr.push(SLBool(isFalse(fr.pop())))
					Opcodes.EQUAL -> fr.push(SLBool(fr.pop() == fr.pop()))
					Opcodes.GREATER -> {
						if(fr.peek() !is SLNumber || fr.peek(1) !is SLNumber){
							runtimeError("Operands must be a number.")
							return
						}
						fr.push(SLBool(fr.pop() as SLNumber > fr.pop() as SLNumber))
					}
					Opcodes.LESS -> {
						if(fr.peek() !is SLNumber || fr.peek(1) !is SLNumber){
							runtimeError("Operands must be a number.")
							return
						}
						fr.push(SLBool((fr.pop() as SLNumber) < (fr.pop() as SLNumber)))
					}
					Opcodes.PRINT -> println(fr.pop())
					Opcodes.POP -> fr.pop()
					Opcodes.DEF_GLOBAL -> {
						val constant = readConstant(fr) as SLString
						globals[constant.value] = fr.pop()
					}
					Opcodes.SET_GLOBAL -> {
						val constant = readConstant(fr) as SLString
						if(!globals.containsKey(constant.value)) {
							runtimeError("Undefined variable '${constant.value}'.")
							return
						}
						globals[constant.value] = fr.peek()
					}
					Opcodes.GET_GLOBAL -> {
						val constant = readConstant(fr) as SLString
						if(!globals.containsKey(constant.value)) {
							runtimeError("Undefined variable '${constant.value}'.")
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
						val typeArgCount: Int = readByte(fr)
						if(!callValue(fr.peek(argCount+typeArgCount), argCount, typeArgCount)){
							return
						}
						fr = frameStack.peek()
						currentFrame = fr
					}
					Opcodes.CLOSURE -> {
						val constant = readConstant(fr) as SLFuncObj
						val closure = SLClosure(constant.value)
						fr.push(SLClosureObj(closure))
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
						fr.push(fr.closure.upvalues[slot]?.closedValue ?: SLNil)
					}
					Opcodes.SET_UPVALUE -> {
						val slot = readShort(fr)
						fr.closure.upvalues[slot]?.closedValue = fr.peek(0);
					}
					Opcodes.CLASS -> {
						val constant = readConstant(fr) as SLString
						val isAbstract = fr.pop() as SLBool
						fr.push(SLClassObj(SLClass(constant.value, mutableMapOf(), mutableMapOf(), mutableMapOf(), mutableMapOf(), isAbstract.value)))
					}

					Opcodes.SET_PROP -> {
						if (fr.peek(1) !is SLClassInstanceObj && fr.peek(1) !is SLClassObj) {
							runtimeError("Only classes or class instances have properties (got ${fr.peek(1)}).")
							return
						}
						if(fr.peek(1) is SLClassObj){
							val clazz = (fr.peek(1) as SLClassObj).value
							val name = readString(fr).value
							val value = fr.pop()
							clazz.staticFields[name]!!.value = value
							fr.pop()
							fr.push(value)
						} else {
							val instance = (fr.peek(1) as SLClassInstanceObj).value
							val name = readString(fr).value
							val value = fr.pop()
							instance.fields[name]!!.value = value
							fr.pop()
							fr.push(value)
						}
					}
					Opcodes.GET_PROP -> {
						if (fr.peek(0) !is SLClassInstanceObj && fr.peek(0) !is SLClassObj) {
							runtimeError("Only classes or class instances have properties.")
							return
						}
						if(fr.peek(0) is SLClassObj){
							val clazz = (fr.peek(0) as SLClassObj).value
							val name = readString(fr).value
							if (clazz.staticFields.containsKey(name)) {
								fr.pop()
								fr.push(clazz.staticFields[name]!!.value)
							} else if (clazz.methods.containsKey(name) && clazz.methods[name]?.value?.function?.modifier == FunctionModifier.STATIC) {
								fr.pop()
								fr.push(clazz.methods[name]!!)
							} else if(clazz.methods[name]?.value?.function?.modifier == FunctionModifier.STATIC_NATIVE && bindMethod(fr, clazz, name)) {

							} else {
								runtimeError("Undefined static property '$name'.")
								return
							}
						} else {
							val instance = (fr.peek(0) as SLClassInstanceObj).value
							val name = readString(fr).value
							if (instance.fields.containsKey(name)) {
								fr.pop()
								fr.push(instance.fields[name]!!.value)
							} else if (bindMethod(fr, instance.clazz, name)) {

							} else {
								runtimeError("Undefined property '$name'.")
								return
							}
						}
					}
					Opcodes.ARRAY_GET -> {
						if (fr.peek(0) !is SLArrayObj) {
							runtimeError("Only arrays support the indexing operator.")
							return
						}
						val arr = (fr.pop() as SLArrayObj).value
						val index = fr.pop()
						if (index !is SLNumber) {
							runtimeError("Array index must be a number.")
						}
						fr.push(arr.get((index as SLNumber).value.toInt()))
					}
					Opcodes.ARRAY_SET -> {
						if (fr.peek(0) !is SLArrayObj) {
							runtimeError("Only arrays support the indexing operator.")
							return
						}
						val arr = (fr.pop() as SLArrayObj).value
						val index = fr.pop()
						val value = fr.pop()
						if (index !is SLNumber) {
							runtimeError("Array index must be a number.")
						}
						arr.set((index as SLNumber).value.toInt(), value)
						fr.push(value)

					}
					Opcodes.METHOD -> {
						defineMethod(fr,readString(fr).value)
					}
					Opcodes.FIELD -> {
						defineField(fr, readString(fr).value)
					}
					Opcodes.STATIC_FIELD -> {
						defineStaticField(fr, readString(fr).value)
					}
					Opcodes.TYPE_PARAM -> {
						defineTypeParam(fr, readString(fr).value)
					}
					Opcodes.INHERIT -> {
						val superclass = fr.peek(1)

						if(superclass !is SLClassObj){
							runtimeError("Superclass must be a class.")
							return
						}

						val subclass = fr.peek(0)

						if(subclass !is SLClassObj){
							runtimeError("Only classes support inheritance.")
							return
						}

						subclass.value.methods.putAll(superclass.value.methods)
						fr.pop()
					}
					Opcodes.GET_SUPER -> {
						val name = readString(fr)
						val superclass = fr.pop()

						if(superclass !is SLClassObj){
							runtimeError("Superclass must be a class.")
							return
						}

						if(!bindMethod(fr, superclass.value, name.value)){
							runtimeError("Cannot bind method '${name.value}' to class '$superclass'.")
							return
						}

					}
					Opcodes.THROW -> {
						throwException(frameStack.size-1, fr.pop())
						fr = frameStack.peek()
						currentFrame = fr
					}

					Opcodes.CHECK -> {
						val type = readConstant(fr) as SLType
						val checking = fr.pop()
						val checkingType = Type.fromValue(checking.value, sunlite)
						fr.push(SLBool(Type.contains(type.value, checkingType, sunlite)))
					}
				}
			}
		} catch (e: Exception){
			if(Sunlite.tickMode){
				runtimeError("internal vm error: $e")
				if(stacktrace){
					e.printStackTrace()
				}
				return
			} else {
				throw e
			}
		}
	}

	override fun run(){
		val fr = frameStack.peek()
		currentFrame = fr

		while (fr.pc < fr.closure.function.chunk.code.size) {
			val currentLine = fr.closure.function.chunk.debugInfo.lines[fr.pc]
			val currentFile = fr.closure.function.chunk.debugInfo.file

			if (sunlite.breakpoints[currentFile]?.contains(currentLine) == true && !ignoreBreakpoints) {
				if(lastBreakpointLine != currentLine){
					if(!breakpointHit){
						sunlite.breakpointListeners.forEach { it.breakpointHit(currentLine, currentFile, sunlite) }
						lastBreakpointLine = currentLine
					}

					breakpointHit = true
				}
				if(breakpointHit && continueExecution) {
					breakpointHit = false
					continueExecution = false
				} else if(breakpointHit) {
					/*if(overrideFunction != null){
						val pc = fr.pc
						ignoreBreakpoints = true
						call(SLClosureObj(SLClosure(overrideFunction!!)),0)
						run()
						ignoreBreakpoints = false
						overrideFunction = null
						fr.pc = pc
					}*/
					continue
				}
			}
			tick()
		}
	}

	private fun readString(fr: CallFrame) = readConstant(fr) as SLString
	private fun readConstant(fr: CallFrame) = fr.closure.function.chunk.constants[readShort(fr)]

	private fun captureUpvalue(fr: CallFrame, index: Int): SLUpvalue {
		val value = fr.stack.elementAt(index)
		val found = openUpvalues.find { it.closedValue == value }

		if(found != null){
			return found
		}

		val upvalue = SLUpvalue(value)
		openUpvalues.add(upvalue)
		return upvalue
	}

	private fun defineMethod(fr: CallFrame, name: String) {
		val method = fr.peek(0) as SLClosureObj
		val clazz = (fr.peek(1) as SLClassObj).value
		clazz.methods[name] = method
		fr.pop()
	}

	private fun defineField(fr: CallFrame, name: String) {
		val type = fr.peek(0) as SLType
		val value = fr.peek(1)
		val clazz = (fr.peek(2) as SLClassObj).value
		clazz.fieldDefaults[name] = SLField(type.value, value)
		fr.pop()
		fr.pop()
	}

	private fun defineStaticField(fr: CallFrame, name: String) {
		val type = fr.peek(0) as SLType
		val value = fr.peek(1)
		val clazz = (fr.peek(2) as SLClassObj).value
		clazz.staticFields[name] = SLField(type.value, value)
		fr.pop()
		fr.pop()
	}

	private fun defineTypeParam(fr: CallFrame, name: String) {
		val type = fr.peek(0) as SLType
		val clazz = (fr.peek(1) as SLClassObj).value
		clazz.typeParams[name] = type.value
		fr.pop()
	}


	private fun bindMethod(fr: CallFrame, clazz: SLClass, name: String): Boolean {
		if(!clazz.methods.containsKey(name)) return false

		val bound = SLBoundMethodObj(
			SLBoundMethod(
				clazz.methods[name]!!.value,
				fr.peek(0)
			))
		fr.pop()
		fr.push(bound)
		return true
	}

	private fun callValue(callee: SLValue<*>, argCount: Int, typeArgCount: Int): Boolean {
		if(callee.isObj()){
			when(callee.value) {
				is SLClosure -> {
					return call(callee as SLClosureObj, argCount)
				}
				is SLNativeFunction -> {
					return callNative(callee as SLNativeFuncObj, argCount)
				}
				is SLClass -> {
					if(callee.value.isAbstract) {
						runtimeError("Can't instantiate interface '${callee.value.name}'.")
						return false
					}
					val stack = frameStack.peek().stack
					val fields: MutableMap<String, SLField> = callee.value.fieldDefaults.mapValues { it.value.copy() }.toMutableMap()
					val instance =
						SLClassInstanceObj(SLClassInstance(callee.value, mutableMapOf(), fields))
					stack[stack.size - argCount - 1 - typeArgCount] = instance
					if(callee.value.methods.containsKey("init")){
						val initMethod = callee.value.methods["init"]!!
						val typeArgs = listOf(*Array(typeArgCount) { i -> (frameStack.peek().pop() as SLType).value })
						typeArgs.forEachIndexed { i, it -> instance.value.typeParams[callee.value.typeParams.keys.toList()[i]] = it }
						fields.values.filter { it.type is Type.Parameter }.forEach {
							val typeParamName = (it.type as Type.Parameter).name.lexeme
							instance.value.typeParams[typeParamName]?.let { innerIt -> it.type = innerIt }
						}
						val success = call(initMethod, argCount)
						if(!success) return false
						frameStack.peek().locals.add(0,instance)
						return true
					} else if(argCount != 0){
						runtimeError("Expected 0 arguments but got $argCount.")
						return false
					}
					return true
				}
				is SLBoundMethod -> {
					if(callee.value.receiver !is SLClassInstanceObj && callee.value.receiver !is SLClassObj){
						runtimeError("Invalid receiver '${callee.value.receiver}' for method '${callee.value.method.function.name}'.")
						return false
					}
					if(callee.value.receiver is SLClassInstanceObj){
						if(callee.value.method.function.modifier == FunctionModifier.NATIVE){
							val methodName = "${callee.value.receiver.value.clazz.name}#${callee.value.method.function.name}"
							if (!(globals.containsKey(methodName))) {
								runtimeError("Native method '$methodName' not bound to anything.")
								return false
							}
							if(globals[methodName] !is SLNativeFuncObj){
								runtimeError("Native method '$methodName' bound to invalid value '${globals[methodName]}'.")
								return false
							}
							return callNative(globals[methodName] as SLNativeFuncObj, argCount)
						}
						val success = call(SLClosureObj(callee.value.method), argCount)
						if(!success) return false
						frameStack.peek().locals.add(0,callee.value.receiver)
						return true
					} else if(callee.value.receiver is SLClassObj) {
						if(callee.value.method.function.modifier == FunctionModifier.STATIC_NATIVE){
							val methodName = "${callee.value.receiver.value.name}#${callee.value.method.function.name}"
							if (!(globals.containsKey(methodName))) {
								runtimeError("Native method '$methodName' not bound to anything.")
								return false
							}
							if(globals[methodName] !is SLNativeFuncObj){
								runtimeError("Native method '$methodName' bound to invalid value '${globals[methodName]}'.")
								return false
							}
							return callNative(globals[methodName] as SLNativeFuncObj, argCount)
						} else {
							runtimeError("Can only call static methods on classes.")
							return false
						}
					}
				}
			}
		}
		runtimeError("Can only call functions.")
		return false
	}

	fun callNative(callee: SLNativeFuncObj, argCount: Int): Boolean {
		if(callee.value.arity != -1 && argCount != callee.value.arity){
			runtimeError("Expected ${callee.value.arity} arguments but got ${argCount}.")
			return false
		}

		frameStack.peek().stack.removeAt((frameStack.peek().stack.size-1)-argCount)
		val args = Array(argCount) { i -> frameStack.peek().pop() }
		args.reverse()
		frameStack.peek().push(callee.value.call(this, args))
		return true
	}

	fun call(callee: SLClosureObj, argCount: Int): Boolean {
		if(callee.value.function.modifier == FunctionModifier.ABSTRACT){
			runtimeError("Can't call abstract method '${callee.value.function.name}'.")
			return false
		}

		if(argCount != callee.value.function.arity){
			runtimeError("Expected ${callee.value.function.arity} arguments but got ${argCount}.")
			return false
		}

		if(frameStack.size == MAX_FRAMES){
			runtimeError("Stack overflow.");
			return false
		}

		val locals = mutableListOf<AnySLValue>()
		locals.addAll(Array(callee.value.function.localsCount - argCount) { i -> SLNil })
		locals.addAll(Array(argCount) { i -> frameStack.peek().peek(i) })
		locals.reverse()
		val frame = CallFrame(callee.value, locals)


		frameStack.push(frame)
		return true
	}

	private fun isFalse(value: AnySLValue): Boolean {
		return value is SLNil || value is SLBool && !value.value
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

	fun throwException(index: Int, e: AnySLValue){
		if(index < 0) {
			throw Exception("Unhandled exception in vm: $e")
		}
		val fr = frameStack[index]
		var closest = Integer.MAX_VALUE
		var exceptionHandler: Map.Entry<IntRange, IntRange>? = null
		fr.closure.function.chunk.exceptions.forEach {
			if (it.key.contains(fr.pc)) {
				val distance = it.value.start - fr.pc
				if(closest > distance){
					exceptionHandler = it
					closest = distance
				}
			}
		}
		if(exceptionHandler != null){
			val stack: Stack<CallFrame> = Stack()
			frameStack.forEachIndexed { i, callFrame ->
				if(i <= index) stack.push(callFrame)
			}
			frameStack.clear()
			stack.forEach { frameStack.push(it) }
			if(fr != frameStack.peek()) {
				error("Frames were unwinded incorrectly! $fr != ${frameStack.peek()}")
			}
			fr.stack.clear()
			fr.locals[0] = e
			fr.pc = exceptionHandler.value.start
			if(currentException != null) currentException = null
		} else {
			if(currentException == null){
				exceptionStacktrace.clear()
				exceptionStacktrace.addAll(frameStack)
			}
			currentException = e
			throwException(index-1, e)
		}
	}

	fun runtimeError(message: String) {

		val fr = frameStack.lastOrNull()

		sunlite.printErr(message)

		val sb = StringBuilder()
		for (frame in frameStack) {
			sb.append("\tat ")
			sb.append(frame)
			sb.append("\n")
		}
		sb.append("\n")

		if(fr != null){
			sb.append("stack @ ${fr.closure.function.chunk.debugInfo.file}::${fr.closure.function.chunk.debugInfo.name}: ")
			for (value in fr.stack) {
				sb.append("[ ")
				sb.append(value)
				sb.append(" ]")
			}
			sb.append("\n")
			sb.append("-> ")
			val offset = Disassembler.disassembleInstruction(sb, fr.closure.function.chunk, fr.pc)
		} else {
			sb.append("<empty stack>")
			sb.append("\n")
		}

		if(fr != null){
			sb.append("locals @ ${fr.closure.function.chunk.debugInfo.file}::${fr.closure.function.chunk.debugInfo.name}: ")
			for (value in fr.locals) {
				sb.append("[ ")
				sb.append(value)
				sb.append(" ]")
			}
			if(fr.locals.isEmpty()){
				sb.append("[ ]")
			}
			sb.append("\n")
		}


		sunlite.printErr(sb.toString())

		frameStack.clear()

		if(stacktrace) {
			Exception("sunlite internal error").printStackTrace()
		}

		sunlite.hadRuntimeError = true
	}
}