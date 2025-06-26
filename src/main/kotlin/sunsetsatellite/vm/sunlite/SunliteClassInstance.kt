package sunsetsatellite.vm.sunlite

class SunliteClassInstance(val clazz: SunliteClass, val fields: MutableMap<String, AnySunliteValue>) {

	override fun toString(): String {
		return "<object '${clazz.name}'>"
	}
}