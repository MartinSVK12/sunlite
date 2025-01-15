package sunsetsatellite.vm.code

import sunsetsatellite.vm.descriptor.Descriptor
import sunsetsatellite.vm.descriptor.FieldDescriptorAnalyzer
import sunsetsatellite.vm.descriptor.MethodDescriptorAnalyzer
import sunsetsatellite.vm.exceptions.*
import sunsetsatellite.vm.obj.ArrayRef
import sunsetsatellite.vm.obj.NullRef
import sunsetsatellite.vm.obj.ObjRef
import sunsetsatellite.vm.runtime.VmFrame
import sunsetsatellite.vm.runtime.VmRuntime
import sunsetsatellite.vm.runtime.VmThread
import sunsetsatellite.vm.type.*

abstract class Instruction {
	abstract fun execute(thread: VmThread, frame: VmFrame)

	override fun toString(): String {
		return "Instruction( ${this::class.java.simpleName} )"
	}
}

class nop : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {}
}

class vreturn : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val returnType = MethodDescriptorAnalyzer.analyze(frame.currentMethod.ref.descriptor).returnType
		if (returnType.type != VmTypes.VOID) {
			throw VmTypeCastException("invalid return value: expected '${returnType.ref}' got 'void'")
		}
		thread.retVoid()
	}
}

class areturn : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value = frame.stack.pop()
		val desc = MethodDescriptorAnalyzer.analyze(frame.currentMethod.ref.descriptor).returnType
		VmRuntime.vm.checkCast(value,desc)
		thread.retValue(value)
	}
}

class goto(val address: Int) : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		frame.pc = address
	}
}

class ldc(val constantPoolIndex: Int) : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val constantValue = frame.runtimeConstantPool[constantPoolIndex].createType(frame.runtimeConstantPool)
		frame.stack.push(constantValue)
	}
}

class iconst(val value: Int) : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		frame.stack.push(IntType(value))
	}
}

class fconst(val value: Float) : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		frame.stack.push(FloatType(value))
	}
}

class iadd : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value1 = (frame.stack.pop() as IntType).getTypeValue()
		val value2 = (frame.stack.pop() as IntType).getTypeValue()
		val result = IntType(value1+value2)
		frame.stack.push(result)
	}
}

class isub : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value1 = (frame.stack.pop() as IntType).getTypeValue()
		val value2 = (frame.stack.pop() as IntType).getTypeValue()
		val result = IntType(value1-value2)
		frame.stack.push(result)
	}
}

class idiv : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value1 = (frame.stack.pop() as IntType).getTypeValue()
		val value2 = (frame.stack.pop() as IntType).getTypeValue()
		if(value2 == 0) throw VmArithmeticException("division by 0")
		val result = IntType(value1/value2)
		frame.stack.push(result)
	}
}

class imul : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value1 = (frame.stack.pop() as IntType).getTypeValue()
		val value2 = (frame.stack.pop() as IntType).getTypeValue()
		val result = IntType(value1*value2)
		frame.stack.push(result)
	}
}

class irem : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value1 = (frame.stack.pop() as IntType).getTypeValue()
		val value2 = (frame.stack.pop() as IntType).getTypeValue()
		if(value2 == 0) throw VmArithmeticException("division by 0")
		val result = IntType(value1 - (value1/value2))
		frame.stack.push(result)
	}
}

class ineg : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value1 = (frame.stack.pop() as IntType).getTypeValue()
		val result = IntType(-value1)
		frame.stack.push(result)
	}
}

class fadd : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value1 = (frame.stack.pop() as FloatType).getTypeValue()
		val value2 = (frame.stack.pop() as FloatType).getTypeValue()
		val result = FloatType(value1+value2)
		frame.stack.push(result)
	}
}

class fsub : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value1 = (frame.stack.pop() as FloatType).getTypeValue()
		val value2 = (frame.stack.pop() as FloatType).getTypeValue()
		val result = FloatType(value1-value2)
		frame.stack.push(result)
	}
}

class fdiv : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value1 = (frame.stack.pop() as FloatType).getTypeValue()
		val value2 = (frame.stack.pop() as FloatType).getTypeValue()
		val result = FloatType(value1/value2)
		frame.stack.push(result)
	}
}

class fmul : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value1 = (frame.stack.pop() as FloatType).getTypeValue()
		val value2 = (frame.stack.pop() as FloatType).getTypeValue()
		val result = FloatType(value1*value2)
		frame.stack.push(result)
	}
}

class frem : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value1 = (frame.stack.pop() as FloatType).getTypeValue()
		val value2 = (frame.stack.pop() as FloatType).getTypeValue()
		val result = FloatType(value1 - (value1/value2))
		frame.stack.push(result)
	}
}

class fneg : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value1 = (frame.stack.pop() as FloatType).getTypeValue()
		val result = FloatType(-value1)
		frame.stack.push(result)
	}
}

class iand : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value1 = (frame.stack.pop() as IntType).getTypeValue()
		val value2 = (frame.stack.pop() as IntType).getTypeValue()
		val result = IntType(value1.and(value2))
		frame.stack.push(result)
	}
}

class ior : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value1 = (frame.stack.pop() as IntType).getTypeValue()
		val value2 = (frame.stack.pop() as IntType).getTypeValue()
		val result = IntType(value1.or(value2))
		frame.stack.push(result)
	}
}

class ixor : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value1 = (frame.stack.pop() as IntType).getTypeValue()
		val value2 = (frame.stack.pop() as IntType).getTypeValue()
		val result = IntType(value1.xor(value2))
		frame.stack.push(result)
	}
}

class ishl : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value1 = (frame.stack.pop() as IntType).getTypeValue()
		val value2 = (frame.stack.pop() as IntType).getTypeValue()
		val result = IntType(value1.shl(value2))
		frame.stack.push(result)
	}
}

class ishr : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value1 = (frame.stack.pop() as IntType).getTypeValue()
		val value2 = (frame.stack.pop() as IntType).getTypeValue()
		val result = IntType(value1.shr(value2))
		frame.stack.push(result)
	}
}

class iushr : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value1 = (frame.stack.pop() as IntType).getTypeValue()
		val value2 = (frame.stack.pop() as IntType).getTypeValue()
		val result = IntType(value1.ushr(value2))
		frame.stack.push(result)
	}
}

class iload(val index: Int) : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value = frame.locals[index] as IntType
		frame.stack.push(value)
	}
}

class istore(val index: Int) : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value = frame.stack.pop() as IntType
		frame.locals[index] = value
	}
}

class fload(val index: Int) : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value = frame.locals[index] as FloatType
		frame.stack.push(value)
	}
}

class fstore(val index: Int) : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value = frame.stack.pop() as FloatType
		frame.locals[index] = value
	}
}

class iinc(val index: Int, val amount: Int) : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value = (frame.locals[index] as IntType).getTypeValue()
		frame.locals[index] = IntType(value+amount)
	}
}

class i2f() : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val intValue = (frame.stack.pop() as IntType).getTypeValue()
		val floatValue = FloatType(intValue.toFloat())
		frame.stack.push(floatValue)
	}
}

class f2i() : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val floatValue = (frame.stack.pop() as FloatType).getTypeValue()
		val intValue = IntType(floatValue.toInt())
		frame.stack.push(intValue)
	}
}

enum class Condition{
	EQUAL,
	NOT_EQUAL,
	LESS,
	GREATER,
	LESS_EQUAL,
	GREATER_EQUAL
}

class iif(val address: Int, val condition: Condition) : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value = (frame.stack.pop() as IntType).getTypeValue()
		var branch = false
		when (condition) {
			Condition.EQUAL -> branch = value == 0
			Condition.NOT_EQUAL -> branch = value != 0
			Condition.LESS -> branch = value < 0
			Condition.GREATER -> branch = value > 0
			Condition.LESS_EQUAL -> branch = value <= 0
			Condition.GREATER_EQUAL -> branch = value >= 0
		}
		if(branch) {
			frame.pc = address
		}
	}
}

class iifcmp(val address: Int, val condition: Condition) : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value = (frame.stack.pop() as IntType).getTypeValue()
		val value2 = (frame.stack.pop() as IntType).getTypeValue()
		var branch = false
		when (condition) {
			Condition.EQUAL -> branch = value == value2
			Condition.NOT_EQUAL -> branch = value != value2
			Condition.LESS -> branch = value < value2
			Condition.GREATER -> branch = value > value2
			Condition.LESS_EQUAL -> branch = value <= value2
			Condition.GREATER_EQUAL -> branch = value >= value2
		}
		if(branch) {
			frame.pc = address
		}
	}
}

class ifnull(val address: Int) : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val ref = frame.stack.pop().getTypeValue() as RefType<*>
		if(ref.getTypeValue().getType() == VmReferenceTypes.NULL){
			frame.pc = address
		}
	}
}

class ifnonnull(val address: Int) : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val ref = frame.stack.pop().getTypeValue() as RefType<*>
		if(ref.getTypeValue().getType() != VmReferenceTypes.NULL){
			frame.pc = address
		}
	}
}

class swap() : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value = frame.stack.pop()
		val value2 = frame.stack.pop()

		frame.stack.push(value)
		frame.stack.push(value2)
	}
}

class pop() : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		frame.stack.pop()
	}
}

class new(val constantPoolIndex: Int): Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val className = VmRuntime.vm.getClassNameByIndex(frame.runtimeConstantPool,constantPoolIndex)
		val obj = VmRuntime.vm.newObject(className)
		frame.stack.push(RefType(ObjRef(obj)))
	}
}

class newarray(val componentType: VmTypes) : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value = (frame.stack.pop() as IntType).getTypeValue()
		if(value < 0) throw VmException("cant create negative size array")
		val refType = RefType(ArrayRef(componentType, value))
		VmRuntime.vm.heap.add(refType)
		frame.stack.push(refType)
	}
}

class arraylength() : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val ref = frame.stack.pop().getTypeValue() as RefType<*>
		VmRuntime.vm.checkCast(ref,Descriptor(VmTypes.ARRAY,"*"))
		val array = ref as RefType<ArrayRef>
		frame.stack.push(IntType(array.getTypeValue().getRef().size))
	}

}

class iastore() : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val ref = frame.stack.pop() as RefType<*>
		val indexType = (frame.stack.pop() as IntType)
		val index = indexType.getTypeValue()
		val value = (frame.stack.pop() as IntType)
		VmRuntime.vm.checkCast(ref,Descriptor(VmTypes.ARRAY,"I"))
		VmRuntime.vm.checkCast(indexType,Descriptor(VmTypes.INT,"int"))
		VmRuntime.vm.checkCast(value,Descriptor(VmTypes.INT,"int"))
		val array = (ref as RefType<ArrayRef>).getTypeValue().getRef()
		if(index < 0 || index >= array.size){
			throw VmOutOfBoundsException("index $index out of bounds for array of length ${array.size}")
		}
		array[index] = value
	}
}

class fastore() : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val ref = frame.stack.pop() as RefType<*>
		val indexType = (frame.stack.pop() as IntType)
		val index = indexType.getTypeValue()
		val value = (frame.stack.pop() as FloatType)
		VmRuntime.vm.checkCast(ref,Descriptor(VmTypes.ARRAY,"F"))
		VmRuntime.vm.checkCast(indexType,Descriptor(VmTypes.INT,"int"))
		VmRuntime.vm.checkCast(value,Descriptor(VmTypes.INT,"int"))
		val array = (ref as RefType<ArrayRef>).getTypeValue().getRef()
		if(index < 0 || index >= array.size){
			throw VmOutOfBoundsException("index $index out of bounds for array of length ${array.size}")
		}
		array[index] = value
	}
}

class castore() : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val ref = frame.stack.pop() as RefType<*>
		val indexType = (frame.stack.pop() as IntType)
		val index = indexType.getTypeValue()
		val value = (frame.stack.pop() as CharType)
		VmRuntime.vm.checkCast(ref,Descriptor(VmTypes.ARRAY,"C"))
		VmRuntime.vm.checkCast(indexType,Descriptor(VmTypes.INT,"int"))
		VmRuntime.vm.checkCast(value,Descriptor(VmTypes.INT,"int"))
		val array = (ref as RefType<ArrayRef>).getTypeValue().getRef()
		if(index < 0 || index >= array.size){
			throw VmOutOfBoundsException("index $index out of bounds for array of length ${array.size}")
		}
		array[index] = value
	}
}

class iaload() : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val ref = frame.stack.pop() as RefType<*>
		val indexType = (frame.stack.pop() as IntType)
		val index = indexType.getTypeValue()
		VmRuntime.vm.checkCast(ref,Descriptor(VmTypes.ARRAY,"I"))
		VmRuntime.vm.checkCast(indexType,Descriptor(VmTypes.INT,"int"))
		val array = (ref as RefType<ArrayRef>).getTypeValue().getRef()
		if(index < 0 || index >= array.size){
			throw VmOutOfBoundsException("index $index out of bounds for array of length ${array.size}")
		}
		frame.stack.push(array[index])
	}
}

class faload() : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val ref = frame.stack.pop() as RefType<*>
		val indexType = (frame.stack.pop() as IntType)
		val index = indexType.getTypeValue()
		VmRuntime.vm.checkCast(ref,Descriptor(VmTypes.ARRAY,"F"))
		VmRuntime.vm.checkCast(indexType,Descriptor(VmTypes.INT,"int"))
		val array = (ref as RefType<ArrayRef>).getTypeValue().getRef()
		if(index < 0 || index >= array.size){
			throw VmOutOfBoundsException("index $index out of bounds for array of length ${array.size}")
		}
		frame.stack.push(array[index])
	}
}

class caload() : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val ref = frame.stack.pop() as RefType<*>
		val indexType = (frame.stack.pop() as IntType)
		val index = indexType.getTypeValue()
		VmRuntime.vm.checkCast(ref,Descriptor(VmTypes.ARRAY,"C"))
		VmRuntime.vm.checkCast(indexType,Descriptor(VmTypes.INT,"int"))
		val array = (ref as RefType<ArrayRef>).getTypeValue().getRef()
		if(index < 0 || index >= array.size){
			throw VmOutOfBoundsException("index $index out of bounds for array of length ${array.size}")
		}
		frame.stack.push(array[index])
	}
}

class aastore() : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val ref = frame.stack.pop() as RefType<*>
		val indexType = (frame.stack.pop() as IntType)
		val index = indexType.getTypeValue()
		val value = (frame.stack.pop() as RefType<*>)
		val arrayRef = (ref as RefType<ArrayRef>).getTypeValue()
		if(arrayRef.componentType != VmTypes.REFERENCE){
			throw VmTypeCastException("cannot cast array of type '${arrayRef.componentType.name.lowercase()}' to 'reference'")
		}
		val array = arrayRef.getRef()
		if(index < 0 || index >= array.size){
			throw VmOutOfBoundsException("index $index out of bounds for array of length ${array.size}")
		}
		array[index] = value
	}
}

class aaload() : Instruction() {
	override fun execute(thread: VmThread, frame: VmFrame) {
		val ref = frame.stack.pop() as RefType<*>
		val indexType = (frame.stack.pop() as IntType)
		val index = indexType.getTypeValue()
		val arrayRef = (ref as RefType<ArrayRef>).getTypeValue()
		if(arrayRef.componentType != VmTypes.REFERENCE){
			throw VmTypeCastException("cannot cast array of type '${arrayRef.componentType.name.lowercase()}' to 'reference'")
		}
		val array = arrayRef.getRef()
		if(index < 0 || index >= array.size){
			throw VmOutOfBoundsException("index $index out of bounds for array of length ${array.size}")
		}
		frame.stack.push(array[index])
	}
}

class invokeinit(val constantPoolIndex: Int = -1) : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val objRef = frame.stack.pop() as RefType<ObjRef>
		val obj = objRef.getTypeValue()
		if(obj.isUninitialized()){
			obj.makeInitialized()
			if(constantPoolIndex == -1){
				val method = VmRuntime.vm.findMethodByName(obj.getRef(), "<init>", "()V", obj.getRef().thisClass.ref.name)
				if(method.ref.name != "<init>"){
					throw VmIllegalCallException("method '${method.ref.name}${method.ref.descriptor}' is not an initializer")
				}
				thread.invokeInstance(method,objRef,frame)
				return
			}
			val method = VmRuntime.vm.getInstanceMethodFromIndex(obj.getRef(), frame.runtimeConstantPool, constantPoolIndex)
			if(method.ref.name != "<init>"){
				throw VmIllegalCallException("method '${method.ref.name}${method.ref.descriptor}' is not an initializer")
			}
			thread.invokeInstance(method,objRef,frame)
		}
	}
}
class invokestatic(val constantPoolIndex: Int) : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val method = VmRuntime.vm.getStaticMethodFromIndex(frame.runtimeConstantPool, constantPoolIndex)
		if(method.ref.name == "<clinit>"){
			throw VmIllegalCallException("illegal attempt to explicitly invoke static initializer of '${method.ref.classRef.name}'")
		}
		thread.invokeStatic(method,frame)
	}

}
class invokeinstance(val constantPoolIndex: Int) : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val objRef = frame.stack.pop() as RefType<ObjRef>
		val obj = objRef.getTypeValue().getRef()
		val method = VmRuntime.vm.getInstanceMethodFromIndex(obj, frame.runtimeConstantPool, constantPoolIndex)
		if(method.ref.name == "<init>"){
			throw VmIllegalCallException("illegal attempt to invoke initializer of '${obj.thisClass.ref.name}'")
		}
		thread.invokeInstance(method,objRef,frame)
	}
}
class invokesuper(val constantPoolIndex: Int) : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val objRef = frame.stack.pop() as RefType<ObjRef>
		val obj = objRef.getTypeValue().getRef()
		val method = VmRuntime.vm.getInstanceMethodFromIndex(obj, frame.runtimeConstantPool, constantPoolIndex, true)
		if(method.ref.name != frame.currentMethod.ref.name || method.ref.descriptor != frame.currentMethod.ref.descriptor){
			throw VmIllegalCallException("illegal attempt to invoke super method from method that doesn't override it")
		}
		thread.invokeInstance(method,objRef,frame)
	}
}

class newnull() : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		frame.stack.push(RefType(NullRef))
	}
}

class dup(): Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value = frame.stack.pop()
		frame.stack.push(value)
		frame.stack.push(value)
	}
}

class putstatic(val constantPoolIndex: Int) : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val field = VmRuntime.vm.getStaticFieldFromIndex(frame.runtimeConstantPool,constantPoolIndex)
		val value = frame.stack.pop()

		if(field.accessFlags.FINAL){
			throw VmIllegalCallException("cannot reassign final static field '${field.ref.name}${field.ref.descriptor}'")
		}

		val desc = FieldDescriptorAnalyzer.analyze(field.ref.descriptor)
		VmRuntime.vm.checkCast(value,desc)

		field.value = value
	}

}
class getstatic(val constantPoolIndex: Int) : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val field = VmRuntime.vm.getStaticFieldFromIndex(frame.runtimeConstantPool,constantPoolIndex)
		frame.stack.push(field.value)
	}

}

class putfield(val constantPoolIndex: Int) : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val obj = ((frame.stack.pop() as RefType<*>).getTypeValue() as ObjRef).getRef()
		val value = frame.stack.pop()

		val field = VmRuntime.vm.getInstanceFieldByIndex(obj, frame.runtimeConstantPool, constantPoolIndex)

		if(field.accessFlags.FINAL){
			throw VmIllegalCallException("cannot reassign final field '${field.ref.name}${field.ref.descriptor}'")
		}

		val desc = FieldDescriptorAnalyzer.analyze(field.ref.descriptor)
		VmRuntime.vm.checkCast(value,desc)

		field.value = value
	}

}
class getfield(val constantPoolIndex: Int) : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val obj = ((frame.stack.pop() as RefType<*>).getTypeValue() as ObjRef).getRef()
		val field = VmRuntime.vm.getInstanceFieldByIndex(obj!!, frame.runtimeConstantPool, constantPoolIndex)
		frame.stack.push(field.value)
	}
}

class athrow() : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val e = frame.stack.pop()
		VmRuntime.vm.checkCast(e, Descriptor(VmTypes.REFERENCE,"base/Throwable"))
		thread.throwException(e as RefType<ObjRef>)
	}

}

class aload(val index: Int) : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value = frame.locals[index] as RefType<*>
		frame.stack.push(value)
	}
}

class astore(val index: Int) : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value = frame.stack.pop() as RefType<*>
		frame.locals[index] = value
	}
}

class instanceof(val constantPoolIndex: Int) : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val value = frame.stack.pop() as RefType<*>
		if (value.getTypeValue().getType() != VmReferenceTypes.OBJECT) {
			frame.stack.push(IntType(0))
			return
		}
		val className = VmRuntime.vm.getClassNameByIndex(frame.runtimeConstantPool, constantPoolIndex)
		val result = VmRuntime.vm.isSuperOrThisClass(className, (value as RefType<ObjRef>).getTypeValue().getRef())
		frame.stack.push(IntType(if(result) 1 else 0))
	}
}

class breakpoint() : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		try {
			throw VmBreakpoint()
		} catch (_: VmBreakpoint){
			System.err.println("breakpoint hit at pc: ${frame.pc} in ${frame.currentMethod}")
		}
	}
}

class debug(val s: String = "") : Instruction(){
	override fun execute(thread: VmThread, frame: VmFrame) {
		val ref = thread.frameStack.peek().currentMethod.ref
		if(VmRuntime.debug){
			println("Debug: $s | S: ${if(frame.stack.isNotEmpty()) frame.stack.peek() else null} | T: ${System.currentTimeMillis()} | M: ${ref.classRef.name}::${ref.name}${ref.descriptor} | PC: ${thread.frameStack.peek().pc}")
		} else {
			println("$s ${if(frame.stack.isNotEmpty()) frame.stack.peek() else null}")
		}
	}
}

