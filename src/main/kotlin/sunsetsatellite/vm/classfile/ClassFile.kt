package sunsetsatellite.vm.classfile

import sunsetsatellite.vm.exceptions.VmFieldNotFoundError
import sunsetsatellite.vm.exceptions.VmMethodNotFoundError
import sunsetsatellite.vm.field.FieldInfo
import sunsetsatellite.vm.method.MethodInfo
import sunsetsatellite.vm.runtime.ConstantPool

class ClassFile (
	val major_version: Int,
	val minor_version: Int,
	val constant_pool_count: Int,
	val access_flags: ClassAccessFlags,
	val this_class: Int,
	val super_class: Int,
	val interfaces_count: Int,
	val fields_count: Int,
	val methods_count: Int,
	val attributes_count: Int,
	val constant_pool: ConstantPool,
	val interfaces: IntArray,
	val fields: Array<FieldInfo>,
	val methods: Array<MethodInfo>,
	val attributes: Array<BaseAttribute>
) {
	val magic: Long = 0xDEADC0DE;

	fun getClassName(): String {
		return constant_pool[this_class].let {
			constant_pool[(it as ConstantInfoClass).nameIndex].let {
				(it as ConstantInfoUTF8).value
			}
		}
	}

	fun getSuperClassName(): String {
		if(super_class == -1) return "null"
		return constant_pool[super_class].let {
			constant_pool[(it as ConstantInfoClass).nameIndex].let {
				(it as ConstantInfoUTF8).value
			}
		}
	}

	fun getMethodInfo(methodName: String, methodDescriptor: String): MethodInfo {
		methods.forEach {
			val name: String = (constant_pool[(constant_pool[it.name_and_type_index] as ConstantInfoNameAndType).nameIndex] as ConstantInfoUTF8).value
			val desc: String = (constant_pool[(constant_pool[it.name_and_type_index] as ConstantInfoNameAndType).descriptorIndex] as ConstantInfoUTF8).value

			if(name == methodName && methodDescriptor == desc){
				return it;
			}
		}
		throw VmMethodNotFoundError(methodName);
	}

	fun getMethodNameAndType(methodInfo: MethodInfo): Pair<String,String> {
		val name: String = (constant_pool[(constant_pool[methodInfo.name_and_type_index] as ConstantInfoNameAndType).nameIndex] as ConstantInfoUTF8).value
		val desc: String = (constant_pool[(constant_pool[methodInfo.name_and_type_index] as ConstantInfoNameAndType).descriptorIndex] as ConstantInfoUTF8).value
		return name to desc
	}

	fun getFieldNameAndType(fieldInfo: FieldInfo): Pair<String,String> {
		val name: String = (constant_pool[(constant_pool[fieldInfo.name_and_type_index] as ConstantInfoNameAndType).nameIndex] as ConstantInfoUTF8).value
		val desc: String = (constant_pool[(constant_pool[fieldInfo.name_and_type_index] as ConstantInfoNameAndType).descriptorIndex] as ConstantInfoUTF8).value
		return name to desc
	}


	fun getFieldInfo(fieldName: String, fieldDescriptor: String): FieldInfo {
		fields.forEach {
			val name: String = (constant_pool[(constant_pool[it.name_and_type_index] as ConstantInfoNameAndType).nameIndex] as ConstantInfoUTF8).value
			val desc: String = (constant_pool[(constant_pool[it.name_and_type_index] as ConstantInfoNameAndType).descriptorIndex] as ConstantInfoUTF8).value

			if(name == fieldName && fieldDescriptor == desc){
				return it;
			}
		}
		throw VmFieldNotFoundError(fieldName);
	}
}