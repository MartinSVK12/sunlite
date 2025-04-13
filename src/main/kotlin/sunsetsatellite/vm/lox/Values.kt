package sunsetsatellite.vm.lox

typealias AnyLoxValue = LoxValue<*>

abstract class LoxValue<T>(val value: T) {
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

class LoxBool(value: Boolean) : LoxValue<Boolean>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is LoxBool) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}
}
class LoxNumber(value: Double) : LoxValue<Double>(value) {
	operator fun unaryMinus(): LoxValue<*> {
		return LoxNumber(-value)
	}

	operator fun plus(other: LoxNumber): LoxValue<*> {
		return LoxNumber(this.value + other.value)
	}

	operator fun minus(other: LoxNumber): LoxValue<*> {
		return LoxNumber(this.value - other.value)
	}

	operator fun times(other: LoxNumber): LoxValue<*> {
		return LoxNumber(this.value * other.value)
	}

	operator fun div(other: LoxNumber): LoxValue<*> {
		return LoxNumber(this.value / other.value)
	}

	override fun equals(other: Any?): Boolean {
		if (other !is LoxNumber) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}

	operator fun compareTo(other: LoxNumber): Int {
		return other.value.compareTo(value)
	}
}

object LoxNil : LoxValue<Unit>(Unit) {
	override fun equals(other: Any?): Boolean {
		return other is LoxNil
	}

	override fun toString(): String {
		return "<nil>"
	}
}
abstract class LoxObj<T>(value: T): LoxValue<T>(value) {
	fun isString(): Boolean {
		return value is String
	}

	fun isFunc(): Boolean {
		return value is LoxFunction
	}
}

class LoxString(value: String) : LoxObj<String>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is LoxString) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}

	operator fun plus(loxString: LoxString): LoxValue<*> {
		return LoxString(this.value + loxString.value)
	}

	override fun toString(): String {
		return "\"${this.value}\""
	}
}

class LoxFuncObj(value: LoxFunction) : LoxObj<LoxFunction>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is LoxFuncObj) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}
}

class LoxClosureObj(value: LoxClosure) : LoxObj<LoxClosure>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is LoxClosureObj) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}
}

class LoxUpvalueObj(value: LoxUpvalue) : LoxObj<LoxUpvalue>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is LoxUpvalue) return false
		return super.equals(other)
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}

	override fun toString(): String {
		return "<upvalue>"
	}
}

class LoxNativeFuncObj(value: LoxNativeFunction) : LoxObj<LoxNativeFunction>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is LoxNativeFuncObj) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}
}