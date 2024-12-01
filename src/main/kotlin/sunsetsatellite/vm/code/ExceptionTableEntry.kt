package sunsetsatellite.vm.code

data class ExceptionTableEntry(val startPc: Int, val endPc: Int, val handlerPc: Int, val catchType: Int)