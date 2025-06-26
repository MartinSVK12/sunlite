package sunsetsatellite.vm.sunlite

typealias AnySunliteValue = SLValue<*>

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

	override fun toString(): String {
		return value.toString()
	}
}

class SLBool(value: Boolean) : SLValue<Boolean>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is SLBool) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}
}
class SLNumber(value: Double) : SLValue<Double>(value) {
	operator fun unaryMinus(): SLValue<*> {
		return SLNumber(-value)
	}

	operator fun plus(other: SLNumber): SLValue<*> {
		return SLNumber(this.value + other.value)
	}

	operator fun minus(other: SLNumber): SLValue<*> {
		return SLNumber(this.value - other.value)
	}

	operator fun times(other: SLNumber): SLValue<*> {
		return SLNumber(this.value * other.value)
	}

	operator fun div(other: SLNumber): SLValue<*> {
		return SLNumber(this.value / other.value)
	}

	override fun equals(other: Any?): Boolean {
		if (other !is SLNumber) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}

	operator fun compareTo(other: SLNumber): Int {
		return other.value.compareTo(value)
	}
}

object SLNil : SLValue<Unit>(Unit) {
	override fun equals(other: Any?): Boolean {
		return other is SLNil
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

	override fun toString(): String {
		return "\"${this.value}\""
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
}

class SLFuncObj(value: SLFunction) : SLObj<SLFunction>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is SLFuncObj) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
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
}

class SLUpvalueObj(value: SLUpvalue) : SLObj<SLUpvalue>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is SLUpvalue) return false
		return super.equals(other)
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
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
}

class SLClassObj(value: SLClass) : SLObj<SLClass>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is SLClassObj) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
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
}

class SLBoundMethodObj(value: SLBoundMethod) : SLObj<SLBoundMethod>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is SLBoundMethodObj) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}
}