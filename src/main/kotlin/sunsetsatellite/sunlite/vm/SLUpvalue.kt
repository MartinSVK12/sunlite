package sunsetsatellite.sunlite.vm

class SLUpvalue(var closedValue: AnySLValue) {
    fun copy(): SLUpvalue {
        return SLUpvalue(closedValue.copy())
    }
}