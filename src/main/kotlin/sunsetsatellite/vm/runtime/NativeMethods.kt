package sunsetsatellite.vm.runtime

import sunsetsatellite.vm.obj.ArrayRef
import sunsetsatellite.vm.obj.ClassInstance
import sunsetsatellite.vm.obj.ResolvedMethod
import sunsetsatellite.vm.runtime.VmRuntime.nativeMethods
import sunsetsatellite.vm.method.NativeMethod
import sunsetsatellite.vm.obj.ObjRef
import sunsetsatellite.vm.type.CharType
import sunsetsatellite.vm.type.RefType
import sunsetsatellite.vm.type.VmType

object NativeMethods {
	fun registerNatives() {
		nativeMethods.putAll(arrayOf(
			"base/StdOut::println(Lbase/String;)V" to NativeMethod { vmThread: VmThread, frame: VmFrame?, resolvedMethod: ResolvedMethod, classInstance: ClassInstance?, args: Array<AnyVmType> ->
				val strObj = args.get(0) as RefType<ObjRef>
				val str = VmRuntime.vm.getStringValue(strObj.getTypeValue().getRef())
				println(str)
			}
		))
	}
}