package sunsetsatellite.vm.obj

import sunsetsatellite.vm.classfile.ClassAccessFlags
import sunsetsatellite.vm.code.Code
import sunsetsatellite.vm.field.FieldAccessFlags
import sunsetsatellite.vm.method.MethodAccessFlags
import sunsetsatellite.vm.type.*

abstract class ResolvedField<T>(val ref: FieldRef<*>, val accessFlags: FieldAccessFlags) {
	abstract var value: VmType<*>
	abstract fun getType(): VmTypes

	override fun toString(): String {
		return "${this::class.java.simpleName}( ${ref.classRef.name}->${ref.name}:${ref.descriptor} )"
	}
}

class ResolvedIntField(ref: FieldRef<Int>, accessFlags: FieldAccessFlags) : ResolvedField<Int>(ref, accessFlags){
	override var value: VmType<*> = IntType(0)
	override fun getType(): VmTypes = VmTypes.INT
}

class ResolvedFloatField(ref: FieldRef<Float>, accessFlags: FieldAccessFlags) : ResolvedField<Float>(ref, accessFlags){
	override var value: VmType<*> = FloatType(0.0f)
	override fun getType(): VmTypes = VmTypes.FLOAT
}

class ResolvedLongField(ref: FieldRef<Long>, accessFlags: FieldAccessFlags) : ResolvedField<Long>(ref, accessFlags){
	override var value: VmType<*> = LongType(0)
	override fun getType(): VmTypes = VmTypes.LONG
}

class ResolvedDoubleField(ref: FieldRef<Double>, accessFlags: FieldAccessFlags) : ResolvedField<Double>(ref, accessFlags){
	override var value: VmType<*> = DoubleType(0.0)
	override fun getType(): VmTypes = VmTypes.DOUBLE
}

class ResolvedCharField(ref: FieldRef<Char>, accessFlags: FieldAccessFlags) : ResolvedField<Char>(ref, accessFlags){
	override var value: VmType<*> = CharType('\u0000')
	override fun getType(): VmTypes = VmTypes.BOOL
}

class ResolvedBoolField(ref: FieldRef<Boolean>, accessFlags: FieldAccessFlags) : ResolvedField<Boolean>(ref, accessFlags){
	override var value: VmType<*> = BoolType(false)
	override fun getType(): VmTypes = VmTypes.BOOL
}

class ResolvedRefField<T>(ref: FieldRef<Reference<*>>, accessFlags: FieldAccessFlags) : ResolvedField<Reference<T>>(ref, accessFlags){
	override var value: VmType<*> = RefType(NullRef) as VmType<Reference<T>>
	override fun getType(): VmTypes = VmTypes.REFERENCE
}

class ResolvedClass(val ref: ClassRef,
                    val major: Int,
                    val minor: Int,
                    val accessFlags: ClassAccessFlags,
                    val superClass: ClassRef?,
                    val interfaces: Array<ClassRef>,
                    val staticMethods: Array<MethodRef>,
                    val staticFields: Array<FieldRef<*>>) {
	override fun toString(): String {
		return "ResolvedClass( ${ref.name} )"
	}
}

class ResolvedMethod(val ref: MethodRef, val accessFlags: MethodAccessFlags, val code: Code){
	override fun toString(): String {
		return "ResolvedMethod( ${ref.classRef.name}::${ref.name}${ref.descriptor} )"
	}
}

