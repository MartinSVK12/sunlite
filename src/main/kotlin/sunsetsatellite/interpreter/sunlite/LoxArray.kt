package sunsetsatellite.interpreter.sunlite

import sunsetsatellite.lang.sunlite.Sunlite
import sunsetsatellite.lang.sunlite.Token
import sunsetsatellite.lang.sunlite.Type

class LoxArray(val type: Type, var size: Int, val sunlite: Sunlite) {
	private var array: Array<Any?> = arrayOfNulls(size)

	fun set(index: Int, value: Any?, token: Token) {
		if(index >= size) {
			throw LoxRuntimeError(token, "Array index $index is out of bounds for an array of size $size.")
		}
		sunlite.typeChecker.checkType(type, Type.fromValue(value, sunlite), token, true)
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