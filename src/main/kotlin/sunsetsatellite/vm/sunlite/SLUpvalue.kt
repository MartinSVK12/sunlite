package sunsetsatellite.vm.sunlite

class SLUpvalue(var closedValue: AnySLValue) {
    fun copy(): SLUpvalue {
        return SLUpvalue(closedValue.copy())
    }
}