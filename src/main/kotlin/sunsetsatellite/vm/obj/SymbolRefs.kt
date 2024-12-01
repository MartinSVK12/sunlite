package sunsetsatellite.vm.obj

import sunsetsatellite.vm.classfile.ClassFile
import sunsetsatellite.vm.runtime.VmRuntime
import sunsetsatellite.vm.type.VmSymbolRefTypes

abstract class SymbolicReference<T> {
	protected var ref: T? = null
	protected var resolved: Boolean = false

	fun get(): T {
		if(!resolved) {
			ref = VmRuntime.vm.refResolver.resolveRef(this)
			return ref!!
		}
		return ref!!
	}

	fun makeResolved(){
		resolved = true
	}

	abstract fun getType(): VmSymbolRefTypes
}

class ClassRef(val name: String, val classDef: ClassFile) : SymbolicReference<ResolvedClass>() {
	override fun getType(): VmSymbolRefTypes = VmSymbolRefTypes.CLASS

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is ClassRef) return false

		if (name != other.name) return false

		return true
	}

	override fun hashCode(): Int {
		return name.hashCode()
	}

}

class FieldRef<T>(val name: String, val descriptor: String, val classRef: ClassRef) : SymbolicReference<ResolvedField<T>>() {
	override fun getType(): VmSymbolRefTypes = VmSymbolRefTypes.FIELD
	override fun toString(): String {
		return "FieldRef( ${classRef.name}->${name}:${descriptor} )"
	}

}

class MethodRef(val name: String, val descriptor: String, val classRef: ClassRef) : SymbolicReference<ResolvedMethod>() {
	override fun getType(): VmSymbolRefTypes = VmSymbolRefTypes.METHOD
	override fun toString(): String {
		return "MethodRef( ${classRef.name}::${name}${descriptor} )"
	}

}