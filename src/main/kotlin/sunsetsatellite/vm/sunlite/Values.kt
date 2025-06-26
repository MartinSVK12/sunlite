package sunsetsatellite.vm.sunlite

typealias AnySunliteValue = SunliteValue<*>

abstract class SunliteValue<T>(val value: T) {
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

	override fun toString(): String {
		return value.toString()
	}
}

class SunliteBool(value: Boolean) : SunliteValue<Boolean>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is SunliteBool) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}
}
class SunliteNumber(value: Double) : SunliteValue<Double>(value) {
	operator fun unaryMinus(): SunliteValue<*> {
		return SunliteNumber(-value)
	}

	operator fun plus(other: SunliteNumber): SunliteValue<*> {
		return SunliteNumber(this.value + other.value)
	}

	operator fun minus(other: SunliteNumber): SunliteValue<*> {
		return SunliteNumber(this.value - other.value)
	}

	operator fun times(other: SunliteNumber): SunliteValue<*> {
		return SunliteNumber(this.value * other.value)
	}

	operator fun div(other: SunliteNumber): SunliteValue<*> {
		return SunliteNumber(this.value / other.value)
	}

	override fun equals(other: Any?): Boolean {
		if (other !is SunliteNumber) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}

	operator fun compareTo(other: SunliteNumber): Int {
		return other.value.compareTo(value)
	}
}

object SunliteNil : SunliteValue<Unit>(Unit) {
	override fun equals(other: Any?): Boolean {
		return other is SunliteNil
	}

	override fun toString(): String {
		return "<nil>"
	}
}
abstract class SunliteObj<T>(value: T): SunliteValue<T>(value) {
	fun isString(): Boolean {
		return value is String
	}

	fun isFunc(): Boolean {
		return value is SunliteFunction
	}
}

class SunliteString(value: String) : SunliteObj<String>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is SunliteString) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}

	operator fun plus(string: SunliteString): SunliteValue<*> {
		return SunliteString(this.value + string.value)
	}

	override fun toString(): String {
		return "\"${this.value}\""
	}
}

class SunliteFuncObj(value: SunliteFunction) : SunliteObj<SunliteFunction>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is SunliteFuncObj) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}
}

class SunliteClosureObj(value: SunliteClosure) : SunliteObj<SunliteClosure>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is SunliteClosureObj) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}
}

class SunliteUpvalueObj(value: SunliteUpvalue) : SunliteObj<SunliteUpvalue>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is SunliteUpvalue) return false
		return super.equals(other)
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}

	override fun toString(): String {
		return "<upvalue>"
	}
}

class SunliteNativeFuncObj(value: SunliteNativeFunction) : SunliteObj<SunliteNativeFunction>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is SunliteNativeFuncObj) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}
}

class SunliteClassObj(value: SunliteClass) : SunliteObj<SunliteClass>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is SunliteClassObj) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}
}

class SunliteClassInstanceObj(value: SunliteClassInstance) : SunliteObj<SunliteClassInstance>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is SunliteClassInstanceObj) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}
}

class SunliteBoundMethodObj(value: SunliteBoundMethod) : SunliteObj<SunliteBoundMethod>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is SunliteBoundMethodObj) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}
}