package sunsetsatellite.vm.sunlite

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
		return value is SunliteFunction
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

class LoxFuncObj(value: SunliteFunction) : LoxObj<SunliteFunction>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is LoxFuncObj) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}
}

class LoxClosureObj(value: SunliteClosure) : LoxObj<SunliteClosure>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is LoxClosureObj) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}
}

class LoxUpvalueObj(value: SunliteUpvalue) : LoxObj<SunliteUpvalue>(value) {
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

class LoxNativeFuncObj(value: SunliteNativeFunction) : LoxObj<SunliteNativeFunction>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is LoxNativeFuncObj) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}
}

class LoxClassObj(value: SunliteClass) : LoxObj<SunliteClass>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is LoxClassObj) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}
}

class LoxClassInstanceObj(value: SunliteClassInstance) : LoxObj<SunliteClassInstance>(value) {
	override fun equals(other: Any?): Boolean {
		if (other !is LoxClassInstanceObj) return false
		return other.value == value
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}
}