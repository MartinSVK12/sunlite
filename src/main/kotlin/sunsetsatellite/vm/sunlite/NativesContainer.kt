package sunsetsatellite.vm.sunlite

interface NativesContainer {
	fun defineNative(function: SLNativeFunction)
	fun getNatives(): Map<String, AnySLValue>
}