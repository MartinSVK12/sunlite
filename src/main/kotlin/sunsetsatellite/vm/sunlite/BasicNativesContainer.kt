package sunsetsatellite.vm.sunlite

class BasicNativesContainer: NativesContainer {
	private val natives: MutableMap<String, AnySLValue> = mutableMapOf()

	override fun defineNative(function: SLNativeFunction) {
		natives[function.name] = SLNativeFuncObj(function)
	}

	override fun getNatives(): Map<String, AnySLValue> {
		return natives
	}
}