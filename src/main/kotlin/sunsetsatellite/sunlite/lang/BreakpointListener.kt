package sunsetsatellite.sunlite.lang

interface BreakpointListener {

    fun breakpointHit(line: Int, file: String?, sunlite: Sunlite)

}