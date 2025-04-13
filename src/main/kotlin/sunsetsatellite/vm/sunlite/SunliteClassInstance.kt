package sunsetsatellite.vm.sunlite

class SunliteClassInstance(val clazz: SunliteClass) {

	override fun toString(): String {
		return "<object '${clazz.name}'>"
	}
}