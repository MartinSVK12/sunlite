package sunsetsatellite.sunlite.vm

import sunsetsatellite.sunlite.lang.Sunlite

class SLTable(val sunlite: Sunlite) {
    private var map: MutableMap<AnySLValue, AnySLValue> = mutableMapOf()

    fun internal(): MutableMap<AnySLValue, AnySLValue> {
        return map
    }

    fun overwrite(m: MutableMap<AnySLValue, AnySLValue>): SLTable {
        this.map = m
        return this
    }

    fun set(index: AnySLValue, value: AnySLValue) {
        map[index] = value
    }

    fun get(index: AnySLValue): AnySLValue {
        return map[index] ?: SLNil
    }

    fun copy(): SLTable {
        return SLTable(sunlite).overwrite(map.mapKeys { it.key.copy() }.mapValues { it.value.copy() }.toMutableMap())
    }

    override fun toString(): String {
        return "<table of size ${map.size}>"
    }
}