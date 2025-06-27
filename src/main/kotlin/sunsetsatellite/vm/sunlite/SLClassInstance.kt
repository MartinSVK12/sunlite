package sunsetsatellite.vm.sunlite

class SLClassInstance(val clazz: SLClass, val fields: MutableMap<String, AnySLValue>) {

	override fun toString(): String {
		return "<object '${clazz.name}'>"
	}

	fun copy(): SLClassInstance {
		return SLClassInstance(clazz.copy(), fields.mapValues { it.value.copy() }.toMutableMap())
	}
}