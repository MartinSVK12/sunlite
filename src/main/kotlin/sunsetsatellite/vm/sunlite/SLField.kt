package sunsetsatellite.vm.sunlite

import sunsetsatellite.lang.sunlite.Type

data class SLField(var type: Type, var value: AnySLValue) {

	fun copy(): SLField {
		return SLField(type, value.copy())
	}

}