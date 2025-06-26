package sunsetsatellite.vm.sunlite

class SLClass(val name: String, val methods: MutableMap<String, SunliteClosureObj>) {

	override fun toString(): String {
		return "<class '${name}'>"
	}
}