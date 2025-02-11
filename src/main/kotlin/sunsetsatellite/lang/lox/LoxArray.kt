package sunsetsatellite.lang.lox

import java.util.*

class LoxArray(val type: Type, var size: Int, val lox: Lox) {
	private var array: Array<Any?> = arrayOfNulls(size)

	fun set(index: Int, value: Any?, token: Token) {
		if(index >= size) {
			throw LoxRuntimeError(token, "Array index $index is out of bounds for an array of size $size.")
		}
		lox.typeChecker.checkType(type, Type.fromValue(value,lox), token, true)
		array[index] = value
	}

	fun get(index: Int, token: Token): Any? {
		if(index >= size) {
			throw LoxRuntimeError(token, "Array index $index is out of bounds for an array of size $size.")
		}
		return array[index]
	}

	fun resize(newSize: Int) {
		val newArray = arrayOfNulls<Any?>(newSize)
		System.arraycopy(array, 0, newArray, 0, Math.min(array.size, newSize))
		size = newSize
		array = newArray
	}

	override fun toString(): String {
		return array.contentToString() //"<array<$type> of size $size>"
	}
}