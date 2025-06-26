package sunsetsatellite.vm.sunlite

class SLClass(val name: String, val methods: MutableMap<String, SLClosureObj>) {

	override fun toString(): String {
		return "<class '${name}'>"
	}
}