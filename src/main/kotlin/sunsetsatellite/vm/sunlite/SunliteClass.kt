package sunsetsatellite.vm.sunlite

class SunliteClass(val name: String, val methods: MutableMap<String, SunliteClosureObj>) {

	override fun toString(): String {
		return "<class '${name}'>"
	}
}