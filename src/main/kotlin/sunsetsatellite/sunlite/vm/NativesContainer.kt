package sunsetsatellite.sunlite.vm

interface NativesContainer {
    fun defineNative(function: SLNativeFunction)
    fun getNatives(): Map<String, AnySLValue>
}