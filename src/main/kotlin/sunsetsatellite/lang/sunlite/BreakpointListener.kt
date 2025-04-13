package sunsetsatellite.lang.sunlite

import sunsetsatellite.interpreter.sunlite.Environment

interface BreakpointListener {

    fun breakpointHit(line: Int, file: String?, sunlite: Sunlite, env: Environment)

}