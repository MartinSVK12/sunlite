package sunsetsatellite.sunlite.vm

import sunsetsatellite.sunlite.lang.Sunlite
import sunsetsatellite.sunlite.lang.Type

class SLArray(var size: Int, val sunlite: Sunlite, val type: Type) {
    private var array: Array<AnySLValue> = VM.arrayOfNils(size)

    fun internal(): Array<AnySLValue> {
        return array
    }

    fun overwrite(arr: Array<AnySLValue>): SLArray {
        array = arr
        return this
    }

    fun set(index: Int, value: AnySLValue) {
        if (index >= size) {
            sunlite.vm.runtimeError("Array index $index is out of bounds for an array of size $size.")
        }
        array[index] = value
    }

    fun get(index: Int): AnySLValue {
        if (index >= size) {
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

    fun copy(): SLArray {
        return SLArray(size, sunlite, type).overwrite(array.map { it.copy() }.toTypedArray())
    }

    override fun toString(): String {
        return "<array of size $size>"
    }
}