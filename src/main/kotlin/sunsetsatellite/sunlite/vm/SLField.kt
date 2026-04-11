package sunsetsatellite.sunlite.vm

import sunsetsatellite.sunlite.lang.Type

data class SLField(var type: Type, var value: AnySLValue) {

	fun copy(): SLField {
		return SLField(type, value.copy())
	}

}