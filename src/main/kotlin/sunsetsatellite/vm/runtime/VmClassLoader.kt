package sunsetsatellite.vm.runtime

import sunsetsatellite.vm.classfile.ClassFile
import sunsetsatellite.vm.exceptions.VmClassNotFoundError

class VmClassLoader{
	//TODO: actual class loading
	fun loadClass(className: String): ClassFile {
		if (!VmRuntime.builtin.containsKey(className)) {
			throw VmClassNotFoundError(className)
		}
		return VmRuntime.builtin[className]!!
	}
}