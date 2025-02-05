package sunsetsatellite.lang.lox

interface BreakpointListener {

    fun breakpointHit(line: Int, file: String?, lox: Lox, env: Environment)

}