package sunsetsatellite.vm.code

open class Code(val maxLocals: Int, val maxStack: Int, val exceptionTable: Array<ExceptionTableEntry>, val inst: Array<Instruction>)

class EmptyCode : Code(0,0, arrayOf(), arrayOf())