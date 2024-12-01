package sunsetsatellite.vm.method

import sunsetsatellite.vm.obj.ClassInstance
import sunsetsatellite.vm.obj.ResolvedMethod
import sunsetsatellite.vm.runtime.AnyVmType
import sunsetsatellite.vm.runtime.VmFrame
import sunsetsatellite.vm.runtime.VmThread

fun interface NativeMethod {
	fun call(thread: VmThread, frame: VmFrame?, method: ResolvedMethod, callingClass: ClassInstance?, args: Array<AnyVmType>)
}