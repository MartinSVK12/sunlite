package sunsetsatellite.sunlite.vm

import sunsetsatellite.sunlite.lang.Type
import kotlin.math.abs

class SLTuple(val vm: VM, val types: List<Type>) {
    private var array: Array<AnySLValue> = VM.arrayOfNils(size)
    private val size: Int
        get() {
            return types.size
        }

    fun internal(): Array<AnySLValue> {
        return array
    }

    fun overwrite(arr: Array<AnySLValue>): SLTuple {
        array = arr
        return this
    }

    fun set(index: Int, value: AnySLValue) {
        if (index >= size || index < -size) {
            vm.runtimeError("Tuple index $index is out of bounds for an tuple of size $size.")
        }
        if(index < 0){
            array[array.size - abs(index)] = value
            return
        }
        array[index] = value
    }

    fun get(index: Int): AnySLValue {
        if (index >= size || index < -size) {
            vm.runtimeError("Tuple index $index is out of bounds for an tuple of size $size.")
        }
        if(index < 0){
            return array[array.size - abs(index)]
        }
        return array[index]
    }


    fun copy(): SLTuple {
        return SLTuple(vm, types).overwrite(array.map { it.copy() }.toTypedArray())
    }

    override fun toString(): String {
        return "<tuple '${types.joinToString(", ")}'>"
    }

}