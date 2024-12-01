package sunsetsatellite.vm.type

import sunsetsatellite.vm.obj.Reference

abstract class VmType<T> {
	protected abstract val value: T
	abstract fun getTypeValue(): T
	abstract fun getType(): VmTypes
	override fun toString(): String {
		return "$value"
	}
}

class IntType(override val value: Int) : VmType<Int>() {
	override fun getTypeValue(): Int = value
	override fun getType(): VmTypes = VmTypes.INT
}

class FloatType(override val value: Float) : VmType<Float>() {
	override fun getTypeValue(): Float = value
	override fun getType(): VmTypes = VmTypes.FLOAT
}

class LongType(override val value: Long) : VmType<Long>() {
	override fun getTypeValue(): Long = value
	override fun getType(): VmTypes = VmTypes.LONG
}

class DoubleType(override val value: Double) : VmType<Double>() {
	override fun getTypeValue(): Double = value
	override fun getType(): VmTypes = VmTypes.DOUBLE
}

class CharType(override val value: Char) : VmType<Char>() {
	override fun getTypeValue(): Char = value
	override fun getType(): VmTypes = VmTypes.CHAR
}

class BoolType(override val value: Boolean) : VmType<Boolean>() {
	override fun getTypeValue(): Boolean = value
	override fun getType(): VmTypes = VmTypes.BOOL
}

class RefType<T : Reference<*>>(override val value: T) : VmType<T>() {
	override fun getTypeValue(): T = value
	override fun getType(): VmTypes = VmTypes.REFERENCE
	override fun toString(): String {
		return "RefType( $value )"
	}
}