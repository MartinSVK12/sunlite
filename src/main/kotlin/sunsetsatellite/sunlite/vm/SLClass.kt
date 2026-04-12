package sunsetsatellite.sunlite.vm

import sunsetsatellite.sunlite.lang.Type

class SLClass(
    val name: String,
    val methods: MutableMap<String, SLClosureObj>,
    val fieldDefaults: MutableMap<String, SLField>,
    val staticFields: MutableMap<String, SLField>,
    val typeParams: MutableMap<String, Type>,
    val isAbstract: Boolean = false
) {

    override fun toString(): String {
        return "<class '${name}'>"
    }

    fun copy(): SLClass {
        return SLClass(
            name,
            methods.mapValues { it.value.copy() as SLClosureObj }.toMutableMap(),
            fieldDefaults.mapValues { it.value.copy() }.toMutableMap(),
            staticFields.mapValues { it.value.copy() }.toMutableMap(),
            typeParams.toMutableMap(),
            isAbstract
        )
    }
}