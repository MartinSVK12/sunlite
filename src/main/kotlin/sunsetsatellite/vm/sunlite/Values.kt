package sunsetsatellite.vm.sunlite

import sunsetsatellite.lang.sunlite.PrimitiveType
import sunsetsatellite.lang.sunlite.Sunlite
import sunsetsatellite.lang.sunlite.Type

typealias AnySLValue = SLValue<*>

abstract class SLValue<T>(val value: T) {
	fun isBool(): Boolean {
		return value is Boolean
	}

	fun isNumber(): Boolean {
		return value is Number
	}

	fun isNil(): Boolean {
		return value is Unit
	}

	fun isObj(): Boolean {
		return !isBool() && !isNumber() && !isNil()
	}

	abstract fun copy(): SLValue<T>

	override fun toString(): String {
		return value.toString()
	}
}

class SLBool : SLValue<Boolean> {

	private constructor(value: Boolean) : super(value)

	companion object {
		val TRUE = SLBool(true)
		val FALSE = SLBool(false)

		fun of(value: Boolean): SLBool {
			return if (value) TRUE else FALSE
		}
	}

	override fun equals(other: Any?): Boolean {
		if (other !is SLBool) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}

	override fun copy(): SLValue<Boolean> {
		return of(value)
	}
}
abstract class SLNumber<T : Number>(value: T) : SLValue<T>(value) {
	abstract operator fun unaryMinus(): SLValue<*>

	abstract operator fun plus(other: SLNumber<*>): SLValue<*>

	abstract operator fun minus(other: SLNumber<*>): SLValue<*>

	abstract operator fun times(other: SLNumber<*>): SLValue<*>

	abstract operator fun div(other: SLNumber<*>): SLValue<*>

	override fun equals(other: Any?): Boolean {
		if (other !is SLNumber<*>) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}

	abstract operator fun compareTo(other: SLNumber<*>): Int
	fun cast(type: Type): SLNumber<*> {
		if(type is Type.Singular){
			val type = type.type;
			when(type){
				PrimitiveType.BYTE -> {
					return SLByte(value.toByte())
				}
				PrimitiveType.SHORT -> {
					return SLShort(value.toShort())
				}
				PrimitiveType.INT -> {
					return SLInt(value.toInt())
				}
				PrimitiveType.LONG -> {
					return SLLong(value.toLong())
				}
				PrimitiveType.FLOAT -> {
					return SLFloat(value.toFloat())
				}
				PrimitiveType.DOUBLE -> {
					return SLDouble(value.toDouble())
				}
				else -> {
					return this
				}
			}
		}
		return this
	}
}

class SLDouble(value: Double) : SLNumber<Double>(value) {
	override fun unaryMinus(): SLValue<*> {
		return SLDouble(-value)
	}

	override fun plus(other: SLNumber<*>): SLValue<*> {
		return SLDouble(value + other.value.toDouble())
	}

	override fun minus(other: SLNumber<*>): SLValue<*> {
		return SLDouble(value - other.value.toDouble())
	}

	override fun times(other: SLNumber<*>): SLValue<*> {
		return SLDouble(value * other.value.toDouble())
	}

	override fun div(other: SLNumber<*>): SLValue<*> {
		return SLDouble(value / other.value.toDouble())
	}

	override fun compareTo(other: SLNumber<*>): Int {
		return value.compareTo(other.value.toDouble())
	}

	override fun copy(): SLValue<Double> {
		return SLDouble(value)
	}
}

class SLFloat(value: Float) : SLNumber<Float>(value) {
	override fun unaryMinus(): SLValue<*> {
		return SLFloat(-value)
	}

	override fun plus(other: SLNumber<*>): SLValue<*> {
		return SLFloat(value + other.value.toFloat())
	}

	override fun minus(other: SLNumber<*>): SLValue<*> {
		return SLFloat(value - other.value.toFloat())
	}

	override fun times(other: SLNumber<*>): SLValue<*> {
		return SLFloat(value * other.value.toFloat())
	}

	override fun div(other: SLNumber<*>): SLValue<*> {
		return SLFloat(value / other.value.toFloat())
	}

	override fun compareTo(other: SLNumber<*>): Int {
		return value.compareTo(other.value.toFloat())
	}

	override fun copy(): SLValue<Float> {
		return SLFloat(value)
	}
}

class SLLong(value: Long) : SLNumber<Long>(value) {
	override fun unaryMinus(): SLValue<*> {
		return SLLong(-value)
	}

	override fun plus(other: SLNumber<*>): SLValue<*> {
		return SLLong(value + other.value.toLong())
	}

	override fun minus(other: SLNumber<*>): SLValue<*> {
		return SLLong(value - other.value.toLong())
	}

	override fun times(other: SLNumber<*>): SLValue<*> {
		return SLLong(value * other.value.toLong())
	}

	override fun div(other: SLNumber<*>): SLValue<*> {
		return SLLong(value / other.value.toLong())
	}

	override fun compareTo(other: SLNumber<*>): Int {
		return value.compareTo(other.value.toLong())
	}

	override fun copy(): SLValue<Long> {
		return SLLong(value)
	}
}

class SLInt(value: Int) : SLNumber<Int>(value) {
	override fun unaryMinus(): SLValue<*> {
		return SLInt(-value)
	}

	override fun plus(other: SLNumber<*>): SLValue<*> {
		return SLInt(value + other.value.toInt())
	}

	override fun minus(other: SLNumber<*>): SLValue<*> {
		return SLInt(value - other.value.toInt())
	}

	override fun times(other: SLNumber<*>): SLValue<*> {
		return SLInt(value * other.value.toInt())
	}

	override fun div(other: SLNumber<*>): SLValue<*> {
		return SLInt(value / other.value.toInt())
	}

	override fun compareTo(other: SLNumber<*>): Int {
		return value.compareTo(other.value.toInt())
	}

	override fun copy(): SLValue<Int> {
		return SLInt(value)
	}
}

class SLShort(value: Short) : SLNumber<Short>(value) {
	override fun unaryMinus(): SLValue<*> {
		return SLShort((-value).toShort())
	}

	override fun plus(other: SLNumber<*>): SLValue<*> {
		return SLShort((value + other.value.toShort()).toShort())
	}

	override fun minus(other: SLNumber<*>): SLValue<*> {
		return SLShort((value - other.value.toShort()).toShort())
	}

	override fun times(other: SLNumber<*>): SLValue<*> {
		return SLShort((value * other.value.toShort()).toShort())
	}

	override fun div(other: SLNumber<*>): SLValue<*> {
		return SLShort((value / other.value.toShort()).toShort())
	}

	override fun compareTo(other: SLNumber<*>): Int {
		return value.compareTo(other.value.toShort())
	}

	override fun copy(): SLValue<Short> {
		return SLShort(value)
	}
}

class SLByte(value: Byte) : SLNumber<Byte>(value) {
	override fun unaryMinus(): SLValue<*> {
		return SLByte((-value).toByte())
	}

	override fun plus(other: SLNumber<*>): SLValue<*> {
		return SLByte((value + other.value.toByte()).toByte())
	}

	override fun minus(other: SLNumber<*>): SLValue<*> {
		return SLByte((value - other.value.toByte()).toByte())
	}

	override fun times(other: SLNumber<*>): SLValue<*> {
		return SLByte((value * other.value.toByte()).toByte())
	}

	override fun div(other: SLNumber<*>): SLValue<*> {
		return SLByte((value / other.value.toByte()).toByte())
	}

	override fun compareTo(other: SLNumber<*>): Int {
		return value.compareTo(other.value.toByte())
	}

	override fun copy(): SLValue<Byte> {
		return SLByte(value)
	}
}

object SLNil : SLValue<Unit>(Unit) {
	override fun equals(other: Any?): Boolean {
		return other is SLNil
	}

	override fun copy(): SLValue<Unit> {
		return SLNil
	}

	override fun toString(): String {
		return "<nil>"
	}
}

abstract class SLObj<T>(value: T): SLValue<T>(value) {
	fun isString(): Boolean {
		return value is String
	}

	fun isFunc(): Boolean {
		return value is SLFunction
	}
}

class SLString(value: String) : SLObj<String>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is SLString) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}

	operator fun plus(string: SLString): SLValue<*> {
		return SLString(this.value + string.value)
	}

	override fun copy(): SLValue<String> {
		return SLString(value)
	}

	override fun toString(): String {
		return "\"${this.value}\""
	}
}
class SLType(value: Type): SLObj<Type>(value){
	override fun equals(other: Any?): Boolean {
		if (other !is SLType) return false
		return Type.contains(other.value, value, Sunlite.instance)
	}

	override fun copy(): SLValue<Type> {
		return SLType(value)
	}

	override fun toString(): String {
		return value.toString()
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}

}
class SLArrayObj(value: SLArray) : SLObj<SLArray>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is SLArrayObj) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}

	override fun copy(): SLValue<SLArray> {
		return SLArrayObj(value.copy())
	}
}
class SLTableObj(value: SLTable) : SLObj<SLTable>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is SLTableObj) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}

	override fun copy(): SLValue<SLTable> {
		return SLTableObj(value.copy())
	}
}
class SLFuncObj(value: SLFunction) : SLObj<SLFunction>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is SLFuncObj) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}

	override fun copy(): SLValue<SLFunction> {
		return SLFuncObj(value.copy())
	}
}
class SLClosureObj(value: SLClosure) : SLObj<SLClosure>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is SLClosureObj) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}

	override fun copy(): SLValue<SLClosure> {
		return SLClosureObj(value.copy())
	}
}
class SLUpvalueObj(value: SLUpvalue) : SLObj<SLUpvalue>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is SLUpvalue) return false
		return super.equals(other)
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}

	override fun copy(): SLValue<SLUpvalue> {
		return SLUpvalueObj(value.copy())
	}

	override fun toString(): String {
		return "<upvalue>"
	}
}
class SLNativeFuncObj(value: SLNativeFunction) : SLObj<SLNativeFunction>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is SLNativeFuncObj) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}

	override fun copy(): SLValue<SLNativeFunction> {
		return SLNativeFuncObj(value)
	}
}
class SLClassObj(value: SLClass) : SLObj<SLClass>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is SLClassObj) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}

	override fun copy(): SLValue<SLClass> {
		return SLClassObj(value.copy())
	}
}
class SLClassInstanceObj(value: SLClassInstance) : SLObj<SLClassInstance>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is SLClassInstanceObj) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}

	override fun copy(): SLValue<SLClassInstance> {
		return SLClassInstanceObj(value.copy())
	}
}
class SLBoundMethodObj(value: SLBoundMethod) : SLObj<SLBoundMethod>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is SLBoundMethodObj) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}

	override fun copy(): SLValue<SLBoundMethod> {
		return SLBoundMethodObj(value.copy())
	}
}