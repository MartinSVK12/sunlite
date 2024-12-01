package sunsetsatellite.vm.method

import sunsetsatellite.vm.classfile.BaseAttribute

class MethodInfo(
	val access_flags: MethodAccessFlags,
	val name_and_type_index: Int,
	val class_index: Int,
	val attributes_count: Int,
	val attributes: Array<BaseAttribute>,
)