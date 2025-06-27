package sunsetsatellite.vm.sunlite

class SLClass(val name: String, val methods: MutableMap<String, SLClosureObj>, val fieldDefaults: MutableMap<String, AnySLValue>) {

	override fun toString(): String {
		return "<class '${name}'>"
	}

	fun copy(): SLClass {
		return SLClass(name,
			methods.mapValues { it.value.copy() as SLClosureObj }.toMutableMap(),
			fieldDefaults.mapValues { it.value.copy() }.toMutableMap()
		)
	}
}