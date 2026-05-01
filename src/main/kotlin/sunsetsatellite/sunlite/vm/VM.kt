package sunsetsatellite.sunlite.vm

import sunsetsatellite.sunlite.lang.*
import sunsetsatellite.sunlite.lang.Scanner
import sunsetsatellite.sunlite.lang.Sunlite.Companion.stacktrace
import java.io.IOException
import java.util.*
import kotlin.collections.filter
import kotlin.io.path.Path

// todo: more runtime checks
class VM(val sunlite: Sunlite, val launchArgs: Array<String>) : Runnable, NativesContainer {

    var ignoreBreakpoints: Boolean = false
    var breakpointHit: Boolean = false
    var continueExecution: Boolean = false
    var lastBreakpointLine: Int = -1

    var currentException: AnySLValue? = null
    val exceptionStacktrace: Stack<CallFrame> = Stack()

    var currentFrame: CallFrame? = null

    val typeChecker = TypeChecker(sunlite, this)

    val imports: MutableMap<String, String> = mutableMapOf()
    val importedClasses: MutableMap<String, SLFunction> = mutableMapOf()
    val globals: MutableMap<String, AnySLValue> = mutableMapOf()
    val primitiveWrappers: MutableMap<Class<out AnySLValue>, String> = mutableMapOf()

    val globalProgramData: MutableMap<String, MutableList<Int>> = mutableMapOf()

    init {
        globals.clear()
        openUpvalues.clear()
        globalProgramData.clear()
        sunlite.natives.registerNatives(this)
        primitiveWrappers[SLString::class.java] = "string"
    }

    companion object {
        const val MAX_FRAMES: Int = 255

        val openUpvalues: MutableList<SLUpvalue> = mutableListOf()

        fun arrayOfNils(size: Int): Array<AnySLValue> {
            return Array(size) { SLNil }
        }
    }

    val frameStack: Stack<CallFrame> = Stack()

    fun <E> Stack<E>.peek(i: Int): E {
        val len = this.size

        if (len == 0) throw EmptyStackException()
        return this.elementAt(len - i - 1)
    }


    override fun defineNative(function: SLNativeFunction) {
        globals[function.name] = SLNativeFuncObj(function)
    }

    override fun getNatives(): Map<String, AnySLValue> {
        return globals
    }

    fun isInitialized(): Boolean {
        return currentFrame != null
    }

    fun tick() {
        try {
            if (currentFrame == null) {
                runtimeError("VM uninitialized.")
                return
            }
            var fr: CallFrame = currentFrame!!
            if (fr.pc < fr.closure.function.chunk.code.size) {
                if (Sunlite.bytecodeDebug) {
                    val sb = StringBuilder()
                    sb.append("STACK @ ${fr.closure.function.chunk.debugInfo.file}::${fr.closure.function.name}${Type.fromValue(fr.closure.function, sunlite).getDescriptor()}: ")
                    for (value in fr.stack) {
                        sb.append("[ ")
                        sb.append(value)
                        sb.append(" ]")
                    }
                    if (fr.stack.isEmpty()) {
                        sb.append("[ ]")
                    }
                    sb.append("\n")
                    sb.append("LOCALS @ ${fr.closure.function.chunk.debugInfo.file}::${fr.closure.function.name}${Type.fromValue(fr.closure.function, sunlite).getDescriptor()}: ")
                    for (value in fr.locals) {
                        sb.append("[ ")
                        sb.append(value)
                        sb.append(" ]")
                    }
                    if (fr.locals.isEmpty()) {
                        sb.append("[ ]")
                    }
                    sb.append("\n")
                    Disassembler.disassembleInstruction(sb, fr.closure.function.chunk, fr.pc)
                    sunlite.printInfo(sb.toString())
                }

                val instruction = readByte(fr)
                when (Opcodes.entries[instruction]) {
                    Opcodes.NOP -> {

                    }

                    Opcodes.RETURN -> {
                        val value: AnySLValue = fr.pop()
                        val type = Type.fromValue(value.value, sunlite)
                        val retType = frameStack.peek().closure.function.returnType
                        val modifier = frameStack.peek().closure.function.modifier
                        if(!frameStack.peek().closure.function.name.contains("init")){
                            typeChecker.checkType(retType, type, true)
                        }

                        if (frameStack.size == 1) {
                            return
                        }
                        val frame = frameStack.pop()
                        fr = frameStack.peek()
                        currentFrame = fr
                        if(modifier != FunctionModifier.CHUNK){
                            for (i in 0 until (frame.closure.function.arity + 1)) {
                                fr.pop()
                            }
                            fr.push(value)
                        }
                    }

                    Opcodes.CONSTANT -> fr.push(readConstant(fr))
                    Opcodes.NEGATE -> {
                        if (fr.peek() !is SLNumber) {
                            runtimeError("Operand must be a number.")
                            return
                        }
                        fr.push(-(fr.pop() as SLNumber))
                    }

                    Opcodes.ADD -> {
                        if (fr.peek() is SLString && fr.peek(1) is SLString) {
                            val right = fr.pop() as SLString
                            val left = fr.pop() as SLString
                            fr.push(left + right)
                        } else if (fr.peek() is SLNumber && fr.peek(1) is SLNumber) {
                            val right = fr.pop() as SLNumber
                            val left = fr.pop() as SLNumber
                            fr.push(left + right)
                        } else {
                            runtimeError("Operands must be numbers or strings.")
                            return
                        }
                    }

                    Opcodes.SUB -> {
                        if (fr.peek() !is SLNumber || fr.peek(1) !is SLNumber) {
                            runtimeError("Operands must be a number.")
                            return
                        }
                        val right = fr.pop() as SLNumber
                        val left = fr.pop() as SLNumber
                        fr.push(left - right)
                    }

                    Opcodes.MULTIPLY -> {
                        if (fr.peek() !is SLNumber || fr.peek(1) !is SLNumber) {
                            runtimeError("Operands must be a number.")
                            return
                        }
                        val right = fr.pop() as SLNumber
                        val left = fr.pop() as SLNumber
                        fr.push(left * right)
                    }

                    Opcodes.DIVIDE -> {
                        if (fr.peek() !is SLNumber || fr.peek(1) !is SLNumber) {
                            runtimeError("Operands must be a number.")
                            return
                        }
                        val right = fr.pop() as SLNumber
                        val left = fr.pop() as SLNumber
                        fr.push(left / right)
                    }

                    Opcodes.REMAINDER -> {
                        if (fr.peek() !is SLNumber || fr.peek(1) !is SLNumber) {
                            runtimeError("Operands must be a number.")
                            return
                        }
                        val right = fr.pop() as SLNumber
                        val left = fr.pop() as SLNumber
                        fr.push(left % right)
                    }

                    Opcodes.NIL -> fr.push(SLNil)
                    Opcodes.TRUE -> fr.push(SLBool.TRUE)
                    Opcodes.FALSE -> fr.push(SLBool.FALSE)
                    Opcodes.NOT -> fr.push(SLBool.of(isFalse(fr.pop())))
                    Opcodes.EQUAL -> fr.push(SLBool.of(fr.pop() == fr.pop()))
                    Opcodes.GREATER -> {
                        if (fr.peek() is SLNumber && fr.peek(1) is SLNumber) {
                            fr.push(SLBool.of((fr.pop() as SLNumber) < (fr.pop() as SLNumber)))
                        } else if(fr.peek() is SLString && fr.peek(1) is SLString){
                            fr.push(SLBool.of((fr.pop() as SLString).value < (fr.pop() as SLString).value))
                        } else {
                            runtimeError("Operands must be numbers or strings.")
                            return
                        }
                    }

                    Opcodes.LESS -> {
                        if (fr.peek() is SLNumber && fr.peek(1) is SLNumber) {
                            fr.push(SLBool.of((fr.pop() as SLNumber) > (fr.pop() as SLNumber)))
                        } else if(fr.peek() is SLString && fr.peek(1) is SLString){
                            fr.push(SLBool.of((fr.pop() as SLString).value > (fr.pop() as SLString).value))
                        } else {
                            runtimeError("Operands must be numbers or strings.")
                            return
                        }
                    }

                    Opcodes.PRINT -> println(fr.pop())
                    Opcodes.POP -> fr.pop()
                    Opcodes.DEF_GLOBAL -> {
                        val constant = readConstant(fr) as SLString
                        globals[constant.value] = fr.pop()
                    }

                    Opcodes.SET_GLOBAL -> {
                        val constant = readConstant(fr) as SLString
                        if (!globals.containsKey(constant.value)) {
                            runtimeError("Undefined variable '${constant.value}'.")
                            return
                        }
                        globals[constant.value] = fr.peek()
                    }

                    Opcodes.GET_GLOBAL -> {
                        val constant = readConstant(fr) as SLString
                        if (!globals.containsKey(constant.value)) {
                            if(!findClass(constant.value)){
                                runtimeError("Undefined variable '${constant.value}'.")
                                return
                            }
                            fr.pc -= 3
                            return
                        }
                        fr.push(globals[constant.value]!!)
                    }

                    Opcodes.SET_LOCAL -> fr.locals[readShort(fr)] = fr.peek()
                    Opcodes.GET_LOCAL -> fr.push(fr.locals[readShort(fr)])
                    Opcodes.JUMP_IF_FALSE -> {
                        val short = readShort(fr)
                        if (isFalse(fr.peek())) {
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
                        val safe: Boolean = (fr.pop() as SLBool).value
                        var argCount: Int = readByte(fr)
                        val typeArgCount: Int = readByte(fr)
                        var func = fr.peek(argCount + typeArgCount)
                        if (func is SLString || func is SLArrayObj) {
                            argCount++
                            func = fr.peek(argCount + typeArgCount)
                        }
                        if(func is SLNil && safe) {
                            fr.pop()
                            for (i in 0 until argCount) {
                                fr.pop()
                            }
                            fr.push(SLNil)
                            return
                        }
                        if (!callValue(func, argCount, typeArgCount)) {
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
                            if (isLocal == 1) {
                                closure.upvalues[i] = captureUpvalue(fr, index)
                            } else {
                                closure.upvalues[i] = fr.closure.upvalues[index]
                            }
                        }
                    }

                    Opcodes.GET_UPVALUE -> {
                        val slot = readShort(fr)
                        fr.push(fr.closure.upvalues[slot]?.closedValue ?: SLNil)
                    }

                    Opcodes.SET_UPVALUE -> {
                        val slot = readShort(fr)
                        fr.closure.upvalues[slot]?.closedValue = fr.peek(0)
                    }

                    Opcodes.CLASS -> {
                        val constant = readConstant(fr) as SLString
                        val isAbstract = fr.pop() as SLBool
                        fr.push(
                            SLClassObj(
                                SLClass(
                                    constant.value,
                                    mutableMapOf(),
                                    mutableMapOf(),
                                    mutableMapOf(),
                                    mutableMapOf(),
                                    isAbstract.value
                                )
                            )
                        )
                    }

                    Opcodes.SET_PROP -> {
                        if (fr.peek(1) !is SLClassInstanceObj && fr.peek(1) !is SLClassObj) {
                            runtimeError("Only classes or class instances have properties (got ${fr.peek(1)}).")
                            return
                        }
                        if (fr.peek(1) is SLClassObj) {
                            val clazz = (fr.peek(1) as SLClassObj).value
                            val name = readString(fr).value
                            val value = fr.pop()
                            typeChecker.checkType(
                                clazz.staticFields[name]!!.type,
                                Type.fromValue(value.value, sunlite),
                                true
                            )
                            clazz.staticFields[name]!!.value = value
                            fr.pop()
                            fr.push(value)
                        } else {
                            val instance = (fr.peek(1) as SLClassInstanceObj).value
                            val name = readString(fr).value
                            val value = fr.pop()
                            typeChecker.checkType(
                                instance.fields[name]!!.type,
                                Type.fromValue(value.value, sunlite),
                                true
                            )
                            instance.fields[name]!!.value = value
                            fr.pop()
                            fr.push(value)
                        }
                    }

                    Opcodes.GET_PROP -> {
                        val safe = (fr.pop() as SLBool).value
                        val arg = fr.peek(0)
                        if (arg !is SLClassInstanceObj && arg !is SLClassObj && arg !is SLArrayObj) {
                            if (!(primitiveWrappers.containsKey(arg.javaClass))) {
                                if(safe && arg is SLNil){
                                    readString(fr).value
                                    fr.pop()
                                    fr.push(SLNil)
                                    return
                                }
                                runtimeError("Only classes or class instances have properties (got ${arg}).")
                                return
                            }
                        }
                        if (arg is SLClassObj) {
                            val clazz = arg.value
                            val name = readString(fr).value
                            if (clazz.staticFields.containsKey(name)) {
                                fr.pop()
                                fr.push(clazz.staticFields[name]!!.value)
                            } else if (clazz.methods.containsKey(name) && clazz.methods[name]?.value?.function?.modifier == FunctionModifier.STATIC) {
                                fr.pop()
                                fr.push(clazz.methods[name]!!)
                            } else if (clazz.methods[name]?.value?.function?.modifier == FunctionModifier.STATIC_NATIVE && bindMethod(
                                    fr,
                                    clazz,
                                    name
                                )
                            ) {

                            } else {
                                runtimeError("Undefined static property '$name'.")
                                return
                            }
                        } else if (arg is SLClassInstanceObj) {
                            val instance = arg.value
                            val name = readString(fr).value
                            if (instance.fields.containsKey(name)) {
                                fr.pop()
                                fr.push(instance.fields[name]!!.value)
                            } else if (bindMethod(fr, instance.clazz, name)) {

                            } else {
                                runtimeError("Undefined property '$name'.")
                                return
                            }
                        } else if (arg is SLArrayObj) {
                            val instance = arg.value
                            val name = readString(fr).value
                            if (globals.containsKey("array")) {
                                val clazz = globals["array"]!!.value as SLClass
                                if (!clazz.methods.containsKey(name)) {
                                    runtimeError("Undefined property '$name'.")
                                    return
                                }
                                fr.pop()
                                val closure = clazz.methods[name]!!
                                fr.push(closure)
                                fr.push(SLArrayObj(instance))
                            } else {
                                runtimeError("InternalError: missing stdlib classes")
                                return
                            }
                        } else if (primitiveWrappers.containsKey(arg.javaClass)) {
                            val wrapperClassName = primitiveWrappers[arg.javaClass]!!
                            val name = readString(fr).value
                            if (globals.containsKey("${wrapperClassName}#${name}")) {
                                fr.pop()
                                fr.push(globals["${wrapperClassName}#${name}"]!!)
                                fr.push(arg)
                            } else {
                                runtimeError("Undefined property '$name'.")
                                return
                            }
                        }
                    }

                    Opcodes.ARRAY_GET -> {
                        if (fr.peek(0) !is SLArrayObj && fr.peek(0) !is SLTableObj && fr.peek(0) !is SLString) {
                            runtimeError("Only arrays, tables and strings support getting with the indexing operator.")
                            return
                        }
                        if(fr.peek(0) is SLString){
                            val s = (fr.pop() as SLString).value
                            val index = fr.pop()
                            if (index !is SLNumber) {
                                runtimeError("String index must be a number.")
                                return
                            }
                            fr.push(SLString(s[index.value.toInt()].toString()))
                        } else if (fr.peek(0) is SLArrayObj) {
                            val arr = (fr.pop() as SLArrayObj).value
                            val index = fr.pop()
                            if (index !is SLNumber) {
                                runtimeError("Array index must be a number.")
                                return
                            }
                            fr.push(arr.get(index.value.toInt()))
                        } else {
                            val arr = (fr.pop() as SLTableObj).value
                            val index = fr.pop()
                            fr.push(arr.get(index))
                        }

                    }

                    Opcodes.ARRAY_SET -> {
                        if (fr.peek(0) !is SLArrayObj && fr.peek(0) !is SLTableObj) {
                            runtimeError("Only arrays or tables support setting with the indexing operator.")
                            return
                        }
                        if (fr.peek(0) is SLArrayObj) {
                            val arr = (fr.pop() as SLArrayObj).value
                            val index = fr.pop()
                            val value = fr.pop()
                            if (index !is SLNumber) {
                                runtimeError("Array index must be a number.")
                            }
                            arr.set((index as SLNumber).value.toInt(), value)
                            fr.push(value)
                        } else {
                            val arr = (fr.pop() as SLTableObj).value
                            val index = fr.pop()
                            val value = fr.pop()
                            arr.set(index, value)
                            fr.push(value)
                        }
                    }

                    Opcodes.METHOD -> {
                        defineMethod(fr, readString(fr).value)
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

                        if (superclass !is SLClassObj) {
                            runtimeError("Superclass must be a class.")
                            return
                        }

                        val subclass = fr.peek(0)

                        if (subclass !is SLClassObj) {
                            runtimeError("Only classes support inheritance.")
                            return
                        }

                        subclass.value.methods.putAll(superclass.value.methods)
                        subclass.value.fieldDefaults.putAll(superclass.value.fieldDefaults)
                        fr.pop()
                        fr.pop()
                    }

                    Opcodes.GET_SUPER -> {
                        val name = readString(fr)
                        val superclass = fr.pop()

                        if (superclass !is SLClassObj) {
                            runtimeError("Superclass must be a class.")
                            return
                        }

                        if (!bindMethod(fr, superclass.value, name.value)) {
                            runtimeError("Cannot bind method '${name.value}' to class '$superclass'.")
                            return
                        }

                    }

                    Opcodes.THROW -> {
                        throwException(frameStack.size - 1, fr.pop() as SLClassInstanceObj)
                        fr = frameStack.peek()
                        currentFrame = fr
                    }

                    Opcodes.CHECK -> {
                        val type = readConstant(fr) as SLType
                        val checking = fr.pop()
                        val checkingType = Type.fromValue(checking.value, sunlite)
                        fr.push(SLBool.of(Type.contains(type.value, checkingType, sunlite)))
                    }

                    Opcodes.CAST -> {
                        val type = readConstant(fr) as SLType
                        val checking = fr.pop()
                        val checkingType = Type.fromValue(checking.value, sunlite)
                        if (checking is SLNumber<*>) {
                            fr.push(checking.cast(type.value))
                        } else {
                            fr.push(checking)
                        }
                        //fr.push(SLBool.of(Type.contains(type.value, checkingType, sunlite)))
                    }

	                Opcodes.SWAP -> {
                        val v1 = fr.pop()
                        val v2 = fr.pop()
                        fr.push(v1)
                        fr.push(v2)
                    }

                    Opcodes.IMPORT -> {
                        val name = (readConstant(fr) as SLString).value
                        val location = (fr.peek() as SLString).value
                        imports[name] = location
                    }
                }
            } else {
                currentFrame = null
            }
        } catch (e: UnhandledException) {
            if (Sunlite.tickMode) {
                printStacktrace("${e.message}")
                return
            } else {
                throw e
            }
        } catch (e: Exception) {
            if (Sunlite.tickMode) {
                runtimeError("InternalError: $e")
                if (stacktrace) {
                    e.printStackTrace()
                }
                return
            } else {
                printStacktrace("InternalError: $e")
                throw e
            }
        }
    }

    override fun run() {
        var fr = frameStack.peek()
        currentFrame = fr

        while (fr.pc < fr.closure.function.chunk.code.size) {
            val currentLine = fr.closure.function.chunk.debugInfo.lines[fr.pc]
            val currentFile = fr.closure.function.chunk.debugInfo.lineData[currentLine]?.let { Path(it) }?.fileName.toString()

            if (sunlite.breakpoints[currentFile]?.contains(currentLine) == true && !ignoreBreakpoints) {
                if (lastBreakpointLine != currentLine) {
                    if (!breakpointHit) {
                        sunlite.breakpointListeners.forEach { it.breakpointHit(currentLine, currentFile, sunlite) }
                        lastBreakpointLine = currentLine
                    }

                    breakpointHit = true
                }
                if (breakpointHit && continueExecution) {
                    breakpointHit = false
                    continueExecution = false
                } else if (breakpointHit) {
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
            fr = currentFrame!!
        }
    }

    private fun readString(fr: CallFrame) = readConstant(fr) as SLString
    private fun readConstant(fr: CallFrame) = fr.closure.function.chunk.constants[readShort(fr)]

    private fun captureUpvalue(fr: CallFrame, index: Int): SLUpvalue {
        val value = fr.locals[index]
        val found = openUpvalues.find { it.closedValue == value }

        if (found != null) {
            return found
        }

        val upvalue = SLUpvalue(value)
        openUpvalues.add(upvalue)
        return upvalue
    }

    private fun defineMethod(fr: CallFrame, name: String) {
        var method = fr.peek(0) as SLClosureObj
        val clazz = (fr.peek(1) as SLClassObj).value
        val function = method.value.function
        if (function.modifier != FunctionModifier.ABSTRACT && clazz.isAbstract) {
            runtimeError("Attempted to define a non-abstract method '$method' on interface '$clazz'.")
            fr.pop()
            return
        }
        clazz.methods[name] = method
        fr.pop()
    }

    private fun reifyFunction(function: SLFunction, typeParams: List<Param>): SLFunction {
        /*val reifiedParams = function.params.map { param ->
            if(!param.type.getDescriptor().contains("G")){
                return@map param
            }
            if (param.type is Type.Reference) {
                val o = object {
                    val primitive = param.type.type
                    val ref = param.type.ref
                    var params = param.type.params
                    var returnType = param.type.returnType
                    val typeParams = mutableListOf(*param.type.typeParams.toTypedArray())
                }
                o.params = o.params.map { refParam ->
                    if (refParam.type is Type.Parameter) {
                        typeParams.find { it.token.lexeme == refParam.type.name.lexeme }?.let {
                            return@map Param(refParam.token, it.type)
                        }
                    }
                    return@map refParam
                }
                o.returnType = o.returnType.let { type ->
                    if (type is Type.Parameter) {
                        val found = typeParams.find { it.token.lexeme == type.name.lexeme }?.let {
                            return@let it.type
                        }
                        return@let found ?: type
                    }
                    return@let type
                }
                return@map Param(param.token, Type.Reference(o.primitive, o.ref, o.returnType, o.params, o.typeParams))
            }
            return@map param
        }
        val reifiedReturnType = function.returnType.let { type ->
            if (type is Type.Parameter) {
                return@let typeParams.find { it.token.lexeme == type.name.lexeme }?.let {
                    return@let it.type
                } ?: type
            } else if(type is Type.Reference){
                val o = object {
                    val primitive = type.type
                    val ref = type.ref
                    var params = type.params
                    var returnType = type.returnType
                    var typeParams = listOf(*type.typeParams.toTypedArray())
                }
                o.params = o.params.map { refParam ->
                    if (refParam.type is Type.Parameter) {
                        typeParams.find { it.token.lexeme == refParam.type.name.lexeme }?.let {
                            return@map Param(refParam.token, it.type)
                        }
                    }
                    return@map refParam
                }
                o.typeParams = o.typeParams.map { refParam ->
                    if (refParam.type is Type.Parameter) {
                        typeParams.find { it.token.lexeme == refParam.type.name.lexeme }?.let {
                            return@map Param(refParam.token, it.type)
                        }
                    }
                    return@map refParam
                }
                o.returnType = o.returnType.let { type ->
                    if (type is Type.Parameter) {
                        val found = typeParams.find { it.token.lexeme == type.name.lexeme }?.let {
                            return@let it.type
                        }
                        return@let found ?: type
                    }
                    return@let type
                }
                var reference = Type.Reference(o.primitive, o.ref, o.returnType, o.params, o.typeParams)
                if(reference.type == PrimitiveType.OBJECT){
                    reference = Type.Reference(o.primitive, o.ref, reference, o.params, o.typeParams)
                }
                return@let reference
            }
            return@let type
        }
        val addedTypeParams = mutableSetOf<Param>()
        addedTypeParams.addAll(function.typeParams)
        //addedTypeParams.addAll(typeParams)
        val function = SLFunction(
            function.name,
            reifiedReturnType,
            reifiedParams,
            addedTypeParams.toList(),
            function.chunk,
            function.arity,
            function.upvalueCount,
            function.localsCount,
            function.modifier
        )*/
        return function
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
        if (!clazz.methods.containsKey(name)) return false

        val receiver = fr.peek(0)
        var method = clazz.methods[name]!!.value

        if(receiver is SLClassInstanceObj){
            val typeParams = receiver.value.typeParams.map { Param(Token.identifier(it.key), it.value) }.toMutableList()
            method = SLClosure(reifyFunction(method.function, typeParams), method.upvalues)
        }

        val bound = SLBoundMethodObj(
            SLBoundMethod(
                method, receiver
            )
        )
        fr.pop()
        fr.push(bound)
        return true
    }

    private fun callValue(callee: SLValue<*>, argCount: Int, typeArgCount: Int): Boolean {
        if (callee.isObj()) {
            when (callee.value) {
                is SLClosure -> {
                    return call(callee as SLClosureObj, argCount, typeArgCount)
                }

                is SLNativeFunction -> {
                    return callNative(callee as SLNativeFuncObj, argCount)
                }

                is SLClass -> {
                    if (callee.value.isAbstract) {
                        runtimeError("Can't instantiate interface '${callee.value.name}'.")
                        return false
                    }
                    val stack = frameStack.peek().stack
                    val fields: MutableMap<String, SLField> =
                        callee.value.fieldDefaults.mapValues { it.value.copy() }.toMutableMap()
                    val instance =
                        SLClassInstanceObj(SLClassInstance(callee.value, mutableMapOf(), fields))
                    stack[stack.size - argCount - 1 - typeArgCount] = instance
                    val typeArgs = listOf(*Array(typeArgCount) { _ -> (frameStack.peek().pop() as SLType).value })
                    /*if(frameStack.peek().locals[0] != SLNil){
                        val functionTypeParams = frameStack.peek().closure.function.typeParams
                        val classTypeParams = (frameStack.peek().locals[0] as SLClassInstanceObj).value.typeParams
                        var i = 0
                        instance.value.clazz.typeParams.forEach { (name, type) ->
                            typeArgs.getOrNull(i)?.let {
                                var found = functionTypeParams.find { f -> f.token.lexeme == it.getName() }?.type
                                if(found == null) {
                                    found = classTypeParams[name]
                                }
                                instance.value.typeParams[name] = found ?: type
                            }
                            i++
                        }
                        //val enclosingTypeParams = frameStack.peek().closure.function.typeParams.map { it.token.lexeme to it.type }.toMap().toMutableMap()
                        //enclosingTypeParams.putAll((frameStack.peek().locals[0] as SLClassInstanceObj).value.typeParams)
                        /*typeArgs.firstOrNull { enclosingTypeParams.contains(it.getName()) }?.let {
                            instance.value.typeParams[it.getName()] = enclosingTypeParams[it.getName()]!!
                        }*/
                        /*enclosingTypeParams.firstNotNullOfOrNull { typeArgs.first { arg -> it.key == arg.getName() } }?.let {
                            instance.value.typeParams[it.getName()] = it
                        }*/
                        /*enclosingTypeParams.forEachIndexed { i, it ->
                            instance.value.typeParams[callee.value.typeParams.keys.toList()[i]] = it
                        }*/
                    } else {
                        typeArgs.forEachIndexed { i, it ->
                            instance.value.typeParams[callee.value.typeParams.keys.toList()[i]] = it
                        }
                    }*/
                    typeArgs.forEachIndexed { i, it ->
                        instance.value.typeParams[callee.value.typeParams.keys.toList()[i]] = it
                    }
                    fields.values.filter {
                        if(it.type is Type.Parameter) {
                            return@filter true
                        } else if(it.type is Type.Union){
                            if((it.type as Type.Union).types.any { innerIt -> innerIt is Type.Parameter }){
                                return@filter true
                            }
                        }
                        return@filter false
                    }.forEach { field ->
                        if(field.type is Type.Parameter){
                            val typeParamName = (field.type as Type.Parameter).name.lexeme
                            instance.value.typeParams[typeParamName]?.let { field.type = it }
                        } else if(field.type is Type.Union){
                            val types = (field.type as Type.Union).types
                            val newTypes = mutableListOf<Type.Singular>()
                            types.forEach {
                                if(it !is Type.Parameter) {
                                    newTypes.add(it)
                                } else {
                                    val typeParamName = it.name.lexeme
                                    val type = instance.value.typeParams[typeParamName]
                                    if(type is Type.Singular){
                                        newTypes.add(type)
                                    } else if(type is Type.Union){
                                        newTypes.addAll(type.types)
                                    }
                                }
                            }
                            field.type = Type.Union(newTypes)
                        }
                    }
                    if (callee.value.methods.keys.any { it.contains("init") }) {
                        val args = Array(argCount) { i -> Param(Type.fromValue(frameStack.peek().peek(i).value, sunlite)) }.reversedArray()
                        val type = Type.ofFunction("", Type.NIL, args.toList())
                        val constructor =
                            callee.value.methods
                                .map { it.value.value.function }
                                .filter { it.name.contains("init") }
                                .filter { it.returnType == Type.NIL }
                                .filter { it.params.size == args.size }
                                .filter { it.params.zip(args).all { (p, a) -> Type.contains(a.type, p.type, sunlite) } }
                        if(constructor.size > 1){
                            runtimeError("Multiple identical constructors defined.")
                            return false
                        }
                        if(constructor.isEmpty()){
                            val availableConstructors =
                                callee.value.methods.keys
                                    .filter { it.contains("init") }
                                    .map{ it.replace("init","") }
                                    .map{ Descriptor(it).getType() }.joinToString("\n\t")
                            runtimeError("Parameters do not match any defined constructor.\nGot ${type}, expected one of\n\t${availableConstructors}\n")
                            return false
                        }
                        val initMethod = callee.value.methods[constructor.first().name]!!
                        val success = call(initMethod, argCount, typeArgCount)
                        if (!success) return false
                        frameStack.peek().locals.add(0, instance)
                        return true
                    } else if (argCount != 0) {
                        runtimeError("Expected 0 arguments but got $argCount.")
                        return false
                    }
                    return true
                }

                is SLBoundMethod -> {
                    if (callee.value.receiver !is SLClassInstanceObj && callee.value.receiver !is SLClassObj) {
                        runtimeError("Invalid receiver '${callee.value.receiver}' for method '${callee.value.method.function.name}'.")
                        return false
                    }
                    if (callee.value.receiver is SLClassInstanceObj) {
                        if (callee.value.method.function.modifier == FunctionModifier.NATIVE) {
                            val methodName =
                                "${callee.value.receiver.value.clazz.name}#${callee.value.method.function.name}"
                            if (!(globals.containsKey(methodName))) {
                                runtimeError("Native method '$methodName' not bound to anything.")
                                return false
                            }
                            if (globals[methodName] !is SLNativeFuncObj) {
                                runtimeError("Native method '$methodName' bound to invalid value '${globals[methodName]}'.")
                                return false
                            }
                            return callNative(globals[methodName] as SLNativeFuncObj, argCount)
                        }
                        val success = call(SLClosureObj(callee.value.method), argCount, typeArgCount, callee.value.receiver)
                        if (!success) return false
                        frameStack.peek().locals.add(0, callee.value.receiver)
                        return true
                    } else if (callee.value.receiver is SLClassObj) {
                        if (callee.value.method.function.modifier == FunctionModifier.STATIC_NATIVE) {
                            val methodName = "${callee.value.receiver.value.name}#${callee.value.method.function.name}"
                            if (!(globals.containsKey(methodName))) {
                                runtimeError("Native method '$methodName' not bound to anything.")
                                return false
                            }
                            if (globals[methodName] !is SLNativeFuncObj) {
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
        if (callee.value.arity != -1 && argCount != callee.value.arity) {
            runtimeError("Expected ${callee.value.arity} arguments but got ${argCount}.")
            return false
        }

        frameStack.peek().stack.removeAt((frameStack.peek().stack.size - 1) - argCount)
        val args = Array(argCount) { i -> frameStack.peek().pop() }
        args.reverse()
        val value = callee.value.call(this, args)
        frameStack.peek().push(value)
        return true
    }

    fun call(callee: SLClosureObj, argCount: Int, typeArgCount: Int = 0, receiverObj: AnySLValue? = null): Boolean {
        val receiver = (receiverObj as SLClassInstanceObj?)?.value
        if (callee.value.function.modifier == FunctionModifier.ABSTRACT) {
            runtimeError("Can't call abstract method '${callee.value.function.name}'.")
            return false
        }

        if (argCount != callee.value.function.arity) {
            runtimeError("Expected ${callee.value.function.arity} arguments but got ${argCount}.")
            return false
        }

        if (frameStack.size == MAX_FRAMES) {
            runtimeError("Stack overflow.")
            return false
        }

        val locals = mutableListOf<AnySLValue>()
        locals.addAll(Array(callee.value.function.localsCount - argCount) { i -> SLNil })
        for (i in 0 until argCount) {
            locals.add(frameStack.peek().peek(typeArgCount + i))
        }
        var functionTypeArgs = mutableListOf<SLType>()
        for (i in 0 until typeArgCount) {
            functionTypeArgs.add(frameStack.peek().pop() as SLType)
        }
        var frameFunction = callee.value
        /*if(receiver != null){
            if(functionTypeArgs.size >= receiver.typeParams.size){
                functionTypeArgs = functionTypeArgs.subList(receiver.typeParams.size, functionTypeArgs.size)
                val list = callee.value.function.typeParams.mapIndexed { index, param ->
                    Param(
                        param.token,
                        functionTypeArgs[index].value
                    )
                }.toList()
                val fullyReified = reifyFunction(callee.value.function, list)
                frameFunction = SLClosure(fullyReified, callee.value.upvalues)

	            frameStack.peek().stack[frameStack.peek().stack.size - argCount - 1] =
		            SLBoundMethodObj(SLBoundMethod(frameFunction, receiverObj))
            }
        }*/
        //locals.addAll(Array(argCount) { i -> frameStack.peek().peek(i - typeArgCount) })
        locals.reverse()
        if (callee.value.function.modifier == FunctionModifier.STATIC) locals.add(0, SLNil)
        val frame = CallFrame(frameFunction, locals)
        frameStack.push(frame)
        return true
    }

    fun findClass(name: String): Boolean {
        if(Sunlite.debug){
            sunlite.printInfo("Finding class: '$name'.")
        }
        importedClasses[name]?.let {
            if(Sunlite.debug){
                sunlite.printInfo("Loading class from cache: '$name'.")
            }
            call(SLClosureObj(SLClosure(it)),0)
            currentFrame = frameStack.peek()
            return true
        }
        imports[name]?.let {
            if(Sunlite.debug){
                sunlite.printInfo("Loading class from file: '$name'.")
            }
	        val chunk = loadFile(it) ?: return false
	        call(chunk,0)
            currentFrame = frameStack.peek()
            return true
        }
        return false
    }

    fun loadFile(file: String): SLClosureObj? {
        var data = Sunlite::class.java.getResourceAsStream(file)?.bufferedReader()?.use { it.readText() }

        if(data == null){
            sunlite.path.forEach {
                try {
                    data = sunlite.readFunction.apply(file)
                } catch (_: IOException) {

                }
            }
        }

        if(data == null){
            return null
        }

        return load(data, file)
    }

    fun load(code: String, path: String = "<loaded chunk>"): SLClosureObj? {
        if (sunlite.collector == null) return null
        val scanner = Scanner(code, sunlite)
        val tokens = scanner.scanTokens(path)
        if (sunlite.hadError) {
            sunlite.hadError = false
            return null
        }
        var parser = Parser(tokens, sunlite, true)
        var statements = parser.parse(path)
        if (sunlite.hadError) {
            sunlite.hadError = false
            return null
        }

        sunlite.collector!!.collect(statements, path, sunlite.compileStep)
        if (sunlite.hadError) {
            sunlite.hadError = false
            return null
        }

        parser = Parser(tokens, sunlite)
        statements = parser.parse(path)
        if (sunlite.hadError) {
            sunlite.hadError = false
            return null
        }

        val checker = TypeChecker(sunlite, this)
        checker.check(statements)
        if (sunlite.hadError) {
            sunlite.hadError = false
            return null
        }

        val compiler = Compiler(sunlite, this, null)
        val program =
            compiler.compile(FunctionType.CHUNK, FunctionModifier.CHUNK, Type.NIL, listOf(), listOf(), statements, path)
        if (sunlite.hadError) {
            sunlite.hadError = false
            return null
        }

        val closure = SLClosure(program)
        return SLClosureObj(closure)
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
        return ((upperByte.toInt() and 0xFF) shl 8 or (lowerByte.toInt() and 0xFF))
    }

    fun internalLoadClass(name: String): SLClassObj {
        try {
            val subVM = VM(sunlite, arrayOf())
            subVM.imports.putAll(imports)
            subVM.importedClasses.putAll(importedClasses)
            subVM.globals.putAll(globals)
            subVM.findClass(name)
            subVM.run()
            return subVM.globals[name] as SLClassObj
        } catch (e: Exception){
            throw Error("InternalError: $e",e)
        }
    }

    fun makeExceptionObject(message: String): SLClassInstanceObj {
        globals["Exception"] = internalLoadClass("Exception")
        val clazz = globals["Exception"] as SLClassObj
        val fields: MutableMap<String, SLField> =
            clazz.value.fieldDefaults.mapValues { it.value.copy() }.toMutableMap()
        fields["message"]?.value = SLString(message)
        val trace = getCurrentStacktrace(false)

        fields["stacktrace"]?.value = SLArrayObj(SLArray(trace.size, sunlite).overwrite(trace as Array<AnySLValue>))
        return SLClassInstanceObj(SLClassInstance(clazz.value, mutableMapOf(), fields))
    }

    fun getCurrentStacktrace(removeFirst: Boolean): Array<SLString> {
        val trace = mutableListOf<String>().apply {
            for ((index, frame) in frameStack.withIndex()) {
                if(index == frameStack.size-1 && removeFirst) continue
                try {
                    add(frame.toString())
                } catch (e: Exception) {
                    add("<error>")
                }
            }
        }.map { SLString(it) }.toTypedArray()
        return trace
    }

    fun throwException(index: Int, e: SLClassInstanceObj) {
        if (index < 0) {
            throw UnhandledException(e)
        }
        val fr = frameStack[index]
        var closest = Integer.MAX_VALUE
        var exceptionHandler: Map.Entry<IntRange, IntRange>? = null
        fr.closure.function.chunk.exceptions.forEach {
            if (it.key.contains(fr.pc)) {
                val distance = it.value.start - fr.pc
                if (closest > distance) {
                    exceptionHandler = it
                    closest = distance
                }
            }
        }
        if (exceptionHandler != null) {
            val stack: Stack<CallFrame> = Stack()
            frameStack.forEachIndexed { i, callFrame ->
                if (i <= index) stack.push(callFrame)
            }
            frameStack.clear()
            stack.forEach { frameStack.push(it) }
            if (fr != frameStack.peek()) {
                error("Frames were unwinded incorrectly! $fr != ${frameStack.peek()}")
            }
            fr.stack.clear()
            fr.locals[0] = e
            fr.pc = exceptionHandler.value.start
            if (currentException != null) currentException = null
        } else {
            if (currentException == null) {
                exceptionStacktrace.clear()
                exceptionStacktrace.addAll(frameStack)
            }
            currentException = e
            throwException(index - 1, e)
        }
    }

    fun getStacktrace(e: SLClassInstanceObj): String {
        val message = e.value.fields["message"]?.value.toString()
        val trace = (e.value.fields["stacktrace"]?.value?.value as SLArray?)?.internal()
        val cause = e.value.fields["cause"]?.value
        val sb = StringBuilder()
        sb.append(e.value.clazz.name).append(": ").append(message).append("\n")
        trace?.forEach {
            sb
                .append("\tat ")
                .append(it.value)
                .append("\n")
        }
        if(cause is SLClassInstanceObj){
	        cause.let {
		        sb.append("Caused by: ")
		        sb.append(getStacktrace(it))
	        }
        }

        return sb.toString()
    }

    fun getStacktrace(e: String): String {
        val fr = frameStack.lastOrNull()
        val sb = StringBuilder()
        sb.append(e).append("\n")
        for (frame in frameStack) {
            try {
                sb.append("\tat ")
                sb.append(frame)
                sb.append("\n")
            } catch (e: Exception) {
                sb.append("<error>")
            }
        }

        if (Sunlite.debug) {
            sb.append("\n")
            try {
                if (fr != null) {
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

                if (fr != null) {
                    sb.append("locals @ ${fr.closure.function.chunk.debugInfo.file}::${fr.closure.function.chunk.debugInfo.name}: ")
                    for (value in fr.locals) {
                        sb.append("[ ")
                        sb.append(value)
                        sb.append(" ]")
                    }
                    if (fr.locals.isEmpty()) {
                        sb.append("[ ]")
                    }
                    sb.append("\n")
                }
            } catch (e: Exception){
                sb.append("\n<error in disassembly>")
            }
        }
        return sb.toString()
    }

    fun printStacktrace(e: String) {
        sunlite.printErr(getStacktrace(e))
    }

    fun printStacktrace(e: SLClassInstanceObj) {
        sunlite.printErr(getStacktrace(e))
    }

    fun runtimeError(message: String) {
        /*if (stacktrace) {
            Exception("runtime error: $message").printStackTrace()
        }*/

        sunlite.hadRuntimeError = true

        throwException(frameStack.size - 1, makeExceptionObject(message))
    }
}