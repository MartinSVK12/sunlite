package sunsetsatellite.sunlite.vm

import sunsetsatellite.sunlite.lang.Type

class SLTable(val vm: VM, val types: Pair<Type, Type>) {
    private var map: MutableMap<AnySLValue, AnySLValue> = mutableMapOf()

    fun internal(): MutableMap<AnySLValue, AnySLValue> {
        return map
    }

    fun overwrite(m: MutableMap<AnySLValue, AnySLValue>): SLTable {
        this.map = m
        return this
    }

    fun set(index: AnySLValue, value: AnySLValue) {
        if(value is SLNil) {
            map.remove(index)
            return
        }
        map[index] = value
    }

    fun get(index: AnySLValue): AnySLValue {
        return map[index] ?: SLNil
    }

    fun copy(): SLTable {
        return SLTable(vm, types).overwrite(map.mapKeys { it.key.copy() }.mapValues { it.value.copy() }.toMutableMap())
    }

    override fun toString(): String {
        return "{${map.map { "${it.key} = ${it.value}" }.joinToString()}}"
    }
}