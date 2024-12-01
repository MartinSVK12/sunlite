package sunsetsatellite.vm.runtime

import sunsetsatellite.vm.classfile.ConstantInfoBase
import sunsetsatellite.vm.obj.ResolvedMethod
import java.util.*

class VmFrame {
	lateinit var locals: Array<AnyVmType>
	lateinit var stack: Stack<AnyVmType>
	lateinit var runtimeConstantPool: ConstantPool
	lateinit var currentMethod: ResolvedMethod
	var finished: Boolean = false
	var suspend: Boolean = false
	var pc = 0
	override fun toString(): String {
		return "Frame( PC: $pc | Locals: ${locals.contentToString()} | Method: $currentMethod | Stack: ${stack} )"
	}
}