package sunsetsatellite.lang.sunlite

interface BreakpointListener {

    fun breakpointHit(line: Int, file: String?, sunlite: Sunlite)

}