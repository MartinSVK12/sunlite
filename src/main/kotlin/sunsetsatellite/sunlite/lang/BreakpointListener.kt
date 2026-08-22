package sunsetsatellite.sunlite.lang

import sunsetsatellite.sunlite.vm.VM

interface BreakpointListener {

    fun breakpointHit(line: Int, file: String?, vm: VM)

}