package sunsetsatellite.sunlite.vm

import sunsetsatellite.sunlite.lang.Type

class SLClassInstance(
    val clazz: SLClass,
    val typeParams: MutableMap<String, Type>,
    val fields: MutableMap<String, SLField>
) {

    override fun toString(): String {
        return "<object '${clazz.name}<${typeParams.map { "${it.key}: ${it.value}" }.joinToString(", ")}>'>"
    }

    fun copy(): SLClassInstance {
        return SLClassInstance(
            clazz.copy(),
            typeParams.toMutableMap(),
            fields.mapValues { it.value.copy() }.toMutableMap()
        )
    }
}