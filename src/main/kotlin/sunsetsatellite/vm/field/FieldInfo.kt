package sunsetsatellite.vm.field

import sunsetsatellite.vm.classfile.BaseAttribute

class FieldInfo(
	val access_flags: FieldAccessFlags,
	val name_and_type_index: Int,
	val class_index: Int,
	val attributes_count: Int,
	val attributes: Array<BaseAttribute>,
)