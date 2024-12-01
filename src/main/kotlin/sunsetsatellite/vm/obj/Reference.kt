package sunsetsatellite.vm.obj

import sunsetsatellite.vm.exceptions.VmException
import sunsetsatellite.vm.exceptions.VmUninitRefAccessError
import sunsetsatellite.vm.runtime.AnyVmType
import sunsetsatellite.vm.type.*

abstract class Reference<T> {
	abstract fun getRef(): T?
	abstract fun getType(): VmReferenceTypes
}

object NullRef : Reference<Nothing?>() {
	override fun getRef(): Nothing? {
		return null
	}

	override fun getType(): VmReferenceTypes = VmReferenceTypes.NULL
	override fun toString(): String {
		return "NullRef"
	}
}

class ObjRef(obj: ClassInstance? = null) : Reference<ClassInstance>() {
	private var instance: ClassInstance? = obj
	private var initialized: Boolean = false

	fun makeInitialized(): ObjRef {
		initialized = true
		return this
	};

	fun isUninitialized(): Boolean{
		return !initialized
	}

	override fun getRef(): ClassInstance {
		if(instance == null || !initialized) throw VmUninitRefAccessError("attempt to access class instance before it has been initialized")
		return instance as ClassInstance
	}

	override fun getType(): VmReferenceTypes = VmReferenceTypes.OBJECT
	override fun toString(): String {
		return "ObjRef( $instance )"
	}
}

class ArrayRef(val componentType: VmTypes, val count: Int) : Reference<Array<VmType<*>>>() {
	private var array: Array<VmType<*>>? = null

	init {
		val typeClass: Class<*> = when (componentType) {
			VmTypes.INT -> IntType::class.java
			VmTypes.FLOAT -> FloatType::class.java
			VmTypes.LONG -> LongType::class.java
			VmTypes.DOUBLE -> DoubleType::class.java
			VmTypes.BOOL -> BoolType::class.java
			VmTypes.REFERENCE -> RefType::class.java
			VmTypes.CHAR -> CharType::class.java
			VmTypes.ARRAY -> throw VmException("multidimensional arrays not supported yet")
			VmTypes.VOID -> throw VmException("cannot make array of type void")
		}
		val defaultValue: Any = when (componentType) {
			VmTypes.INT -> 0
			VmTypes.FLOAT -> 0
			VmTypes.LONG -> 0
			VmTypes.DOUBLE -> 0
			VmTypes.BOOL -> false
			VmTypes.REFERENCE -> NullRef
			VmTypes.CHAR -> '\u0000'
			VmTypes.ARRAY -> throw VmException("multidimensional arrays not supported yet")
			VmTypes.VOID -> throw VmException("cannot make array of type void")
		}
		val list: MutableList<AnyVmType> = mutableListOf()
		for (i in 0 until count) {
			list.add(typeClass.constructors[0].newInstance(defaultValue) as AnyVmType)
		}
		array = list.toTypedArray()
	}

	override fun getRef(): Array<VmType<*>> {
		if(array == null) throw VmUninitRefAccessError("attempt to access array instance before it has been initialized")
		return array as Array<VmType<*>>
	}

	override fun getType(): VmReferenceTypes = VmReferenceTypes.ARRAY
	override fun toString(): String {
		return "ArrayRef( $componentType[$count] | ${array?.contentToString()} )"
	}
}