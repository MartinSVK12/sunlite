package sunsetsatellite.vm.runtime

import sunsetsatellite.vm.descriptor.MethodDescriptorAnalyzer
import sunsetsatellite.vm.exceptions.*
import sunsetsatellite.vm.obj.NullRef
import sunsetsatellite.vm.obj.ObjRef
import sunsetsatellite.vm.obj.ResolvedClass
import sunsetsatellite.vm.obj.ResolvedMethod
import sunsetsatellite.vm.runtime.VmRuntime.maxFrameStackSize
import sunsetsatellite.vm.runtime.VmRuntime.nativeMethods
import sunsetsatellite.vm.type.RefType
import java.util.*

class VmThread(val name: String) : Runnable {
	val frameStack: Stack<VmFrame> = Stack()
	var exceptionStackTrace: Stack<VmFrame> = Stack()
	var currentException: RefType<ObjRef>? = null
	override fun run() {
		while (!frameStack.empty()){
			tick()
		}
		/*try {
			while (!frameStack.empty()) {
				val currentFrame = frameStack.peek()
				val maxPc = currentFrame.currentMethod.code.inst.size

				currentException?.let { throwException(it) }

				while(currentFrame.pc < maxPc) {
					if(currentFrame.finished) break
					if(currentFrame.suspend) break
					try {
						val instruction = currentFrame.currentMethod.code.inst[currentFrame.pc++]
						instruction.execute(this,currentFrame)
						if(frameStack.size > maxFrameStackSize) throw VmStackOverflowError("frame stack exceeded maximum size: $maxFrameStackSize")
					} catch (e: VmException){
						//throwException(RefType(ObjRef(VmRuntime.vm.newThrowable(e.message)).makeInitialized()))
						throw VmInternalError("exception in vm code", e)
					}
				}
				if(currentFrame.suspend) {
					currentFrame.suspend = false
					continue
				}

				if(currentException != null && frameStack.empty()) {
					throw UncaughtVmException("uncaught exception in thread \"${this.name}\"")
				}
				if(!currentFrame.finished){
					currentFrame.finished = true
					frameStack.pop()
				}
			}
			if(currentException != null) {
				throw UncaughtVmException("uncaught exception in thread \"${this.name}\"")
			}
		} catch (e: Throwable){
			printStackTrace(e)
		}*/
	}

	fun tick(){
		try {
			if(!frameStack.empty()){
				val currentFrame = frameStack.peek()
				val maxPc = currentFrame.currentMethod.code.inst.size

				currentException?.let { throwException(it) }

				if(currentFrame.pc < maxPc && !currentFrame.finished && !currentFrame.suspend){
					try {
						val instruction = currentFrame.currentMethod.code.inst[currentFrame.pc++]
						instruction.execute(this,currentFrame)
						if(frameStack.size > maxFrameStackSize) throw VmStackOverflowError("frame stack exceeded maximum size: $maxFrameStackSize")
					} catch (e: VmException){
						//TODO: don't force init like this or maybe do idk
						//FIXME: native stack trace is lost
						//throwException(RefType(ObjRef(VmRuntime.vm.newThrowable(e.message)).makeInitialized()))
						throw VmInternalError("exception in vm code", e)
					}
				} else {
					if(currentFrame.suspend) {
						currentFrame.suspend = false
						return
					}

					if(currentException != null && frameStack.empty()) {
						throw UncaughtVmException("uncaught exception in thread \"${this.name}\"")
					}
					if(!currentFrame.finished){
						currentFrame.finished = true
						frameStack.pop()
					}
				}
			}
			if(currentException != null) {
				throw UncaughtVmException("uncaught exception in thread \"${this.name}\"")
			}
		} catch (e: Throwable){
			printStackTrace(e)
		}
	}

	fun invokeStaticInit(obj: ResolvedClass, frame: VmFrame?){
		val method =
			obj.staticMethods.find { it.name == "<clinit>" }?.get()
				?: throw VmMethodNotFoundError("<clinit>")
		invokeStatic(method, frame)
	}

	fun invokeStatic(method: ResolvedMethod, frame: VmFrame?) {
		if(VmRuntime.debug) println("Calling static method: ${method.ref.classRef.name}::${method.ref.name}${method.ref.descriptor}")
		var args: MutableList<AnyVmType> = mutableListOf()
		if(!frameStack.empty() && frame != null) {
			frame.suspend = true
			val (_, arguments) = MethodDescriptorAnalyzer.analyze(method.ref.descriptor)
			args = arguments.map {
				val value = frame.stack.removeFirst()
				VmRuntime.vm.checkCast(value,it)
				return@map value
			}.toMutableList()
			for (i in 0 until method.code.maxLocals-args.size) {
				args.add(RefType(NullRef))
			}
		} else {
			for (i in 0 until method.code.maxLocals-args.size) {
				args.add(RefType(NullRef))
			}
		}
		invokeInternal(method, null, frameStack.size, args.toTypedArray(), frame)
	}

	fun invokeStaticLater(method: ResolvedMethod, frame: VmFrame?) {
		if(VmRuntime.debug) println("Scheduling a call for static method: ${method.ref.classRef.name}::${method.ref.name}${method.ref.descriptor}")
		var args: MutableList<AnyVmType> = mutableListOf()
		if(!frameStack.empty() && frame != null) {
			val (_, arguments) = MethodDescriptorAnalyzer.analyze(method.ref.descriptor)
			args = arguments.map {
				val value = frame.stack.pop()
				VmRuntime.vm.checkCast(value,it)
				return@map value
			}.toMutableList()
			for (i in 0 until method.code.maxLocals-args.size) {
				args.add(RefType(NullRef))
			}
		} else {
			for (i in 0 until method.code.maxLocals-args.size) {
				args.add(RefType(NullRef))
			}
		}
		invokeInternal(method, null,0, args.toTypedArray(), frame)
	}

	fun invokeInstance(method: ResolvedMethod, thisClass: RefType<ObjRef>, frame: VmFrame) {
		if(VmRuntime.debug) println("Calling instance method: ${method.ref.classRef.name}::${method.ref.name}${method.ref.descriptor}")
		if(!frameStack.empty()) frame.suspend = true
		val (_, arguments) = MethodDescriptorAnalyzer.analyze(method.ref.descriptor)
		val args = arguments.map {
			val value = frame.stack.pop()
			VmRuntime.vm.checkCast(value,it)
			return@map value
		}.toMutableList()
		args.add(0,thisClass)
		for (i in 0 until method.code.maxLocals-args.size) {
			args.add(RefType(NullRef))
		}
		invokeInternal(method, thisClass, frameStack.size, args.toTypedArray(), frame)
	}

	private fun invokeInternal(method: ResolvedMethod, thisClass: RefType<ObjRef>?, index: Int, args: Array<AnyVmType>, frame: VmFrame?) {
		if(method.accessFlags.ABSTRACT){
			throw VmIllegalCallException("illegal attempt to invoke abstract method '${method.ref.name}${method.ref.descriptor}'")
		}
		if(method.accessFlags.NATIVE){
			if(VmRuntime.debug) println("Native method detected: ${method.ref.classRef.name}::${method.ref.name}${method.ref.descriptor}")
			val signature = "${method.ref.classRef.name}::${method.ref.name}${method.ref.descriptor}"
			if (nativeMethods.containsKey(signature)) {
				nativeMethods[signature]?.call(
					thread = this,
					frame = frame,
					method = method,
					callingClass = thisClass?.getTypeValue()?.getRef(),
					args = args
				)
			} else {
				throw VmClassNotFoundError("no native method linked to $signature")
			}
			return
		}
		val nextFrame = VmFrame()
		nextFrame.locals = args
		nextFrame.stack = Stack()
		nextFrame.runtimeConstantPool = VmRuntime.vm.getClassDef(method.ref.classRef.name).constant_pool
		nextFrame.currentMethod = method
		frameStack.add(index,nextFrame)
	}

	fun retVoid(){
		val frame = frameStack.pop()
		frame.finished = true
	}

	fun retValue(type: AnyVmType) {
		val frame = frameStack.pop()
		frame.finished = true;
		if(frameStack.empty()) return
		val nextFrame = frameStack.peek()
		nextFrame.stack.push(type)
	}

	fun throwException(e: RefType<ObjRef>){
		val currentFrame = frameStack.peek()
		try {
			val exceptionHandler = currentFrame.currentMethod.code.exceptionTable.find {
				val className = e.getTypeValue().getRef().thisClass.ref.name
				VmRuntime.vm.getClassNameByIndex(VmRuntime.vm.getClassDef(className).constant_pool,it.catchType) == className &&
				currentFrame.pc >= it.startPc && currentFrame.pc <= it.endPc
			}
			if(exceptionHandler != null){
				currentFrame.stack.clear()
				currentFrame.stack.push(e)
				currentFrame.pc = exceptionHandler.handlerPc
				if(currentException != null){
					currentException = null
					exceptionStackTrace.clear()
				}
			} else {
				if(currentException == null){
					this.exceptionStackTrace.clear()
					this.exceptionStackTrace.addAll(frameStack)
				}
				currentException = e
				retVoid()
			}
		} catch (e: Throwable){
			throw VmInternalError("native exception while trying to throw virtual exception", e)
		}

	}

	fun printStackTrace(e: Throwable){
		System.err.println("Internal exception detected by the virtual runtime environment:")
		System.err.println()
		System.err.println("Virtual stacktrace:")
		if(currentException != null) {
			val virtualE = currentException?.getTypeValue()?.getRef()
			val virtualEMsg = VmRuntime.vm.getThrowableMessage(virtualE)
			System.err.println("${virtualE?.thisClass?.ref?.name}: $virtualEMsg")
			exceptionStackTrace.forEach {
				val currentMethod = it.currentMethod.ref
				System.err.println("    at ${currentMethod.classRef.name}::${currentMethod.name}${currentMethod.descriptor} (builtin:${it.pc-1})")
			}
		} else {
			System.err.println("(no exception)")
			frameStack.forEach {
				val currentMethod = it.currentMethod.ref
				System.err.println("    at ${currentMethod.classRef.name}::${currentMethod.name}${currentMethod.descriptor} (builtin:${it.pc-1})")
			}
		}

		System.err.println()
		System.err.println("Native stacktrace:")
		e.printStackTrace()
	}

	override fun toString(): String {
		return "VmThread( Stack: ${frameStack.size} )"
	}
}