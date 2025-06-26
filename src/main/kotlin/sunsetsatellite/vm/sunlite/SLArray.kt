package sunsetsatellite.vm.sunlite

import sunsetsatellite.lang.sunlite.Sunlite

class SLArray(var size: Int, val sunlite: Sunlite) {
	private var array: Array<AnySLValue> = VM.arrayOfNils(size)

	fun set(index: Int, value: AnySLValue) {
		if(index >= size) {
			sunlite.vm.runtimeError("Array index $index is out of bounds for an array of size $size.")
		}
		array[index] = value
	}

	fun get(index: Int): AnySLValue {
		if(index >= size) {
			sunlite.vm.runtimeError("Array index $index is out of bounds for an array of size $size.")
		}
		return array[index]
	}

	fun resize(newSize: Int) {
		val newArray = VM.arrayOfNils(newSize)
		System.arraycopy(array, 0, newArray, 0, array.size.coerceAtMost(newSize))
		size = newSize
		array = newArray
	}

	override fun toString(): String {
		return "<array of size $size>"
	}
}