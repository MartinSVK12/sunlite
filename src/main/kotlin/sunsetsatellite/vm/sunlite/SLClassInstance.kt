package sunsetsatellite.vm.sunlite

import sunsetsatellite.lang.sunlite.Type

class SLClassInstance(val clazz: SLClass, val typeParams: MutableMap<String, Type>, val fields: MutableMap<String, SLField>) {

	override fun toString(): String {
		return "<object '${clazz.name}'>"
	}

	fun copy(): SLClassInstance {
		return SLClassInstance(clazz.copy(), typeParams.toMutableMap(),fields.mapValues { it.value.copy() }.toMutableMap())
	}
}