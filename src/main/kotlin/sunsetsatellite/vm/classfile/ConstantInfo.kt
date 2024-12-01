package sunsetsatellite.vm.classfile

import sunsetsatellite.vm.exceptions.VmInternalError
import sunsetsatellite.vm.obj.ArrayRef
import sunsetsatellite.vm.obj.ObjRef
import sunsetsatellite.vm.runtime.AnyVmType
import sunsetsatellite.vm.runtime.ConstantPool
import sunsetsatellite.vm.runtime.VmRuntime
import sunsetsatellite.vm.type.*

abstract class ConstantInfoBase {
	abstract fun getType(): ConstantType;

	fun createType(runtimeConstantPool: ConstantPool): AnyVmType{
		when(getType()){
			ConstantType.INT -> return IntType((this as ConstantInfoInt).value)
			ConstantType.FLOAT -> return FloatType((this as ConstantInfoFloat).value)
			ConstantType.LONG -> return LongType(((this as ConstantInfoLong).value))
			ConstantType.DOUBLE -> return DoubleType((this as ConstantInfoDouble).value)
			ConstantType.STRING -> {
				val stringValue = (runtimeConstantPool[(this as ConstantInfoString).stringIndex] as ConstantInfoUTF8).value
				val stringRef = RefType(ObjRef(VmRuntime.vm.newString(stringValue)))
				return stringRef
			};
			else -> throw VmInternalError("cannot create type from ${getType()}")
			/*
			ConstantType.UTF8 ->
			ConstantType.CLASS ->
			ConstantType.FIELD_REF ->
			ConstantType.METHOD_REF ->
			ConstantType.INTERFACE_METHOD_REF ->
			ConstantType.NAME_AND_TYPE ->
           */
		}
	}
}

class ConstantInfoClass(val nameIndex: Int) : ConstantInfoBase() {

	override fun getType(): ConstantType {
		return ConstantType.CLASS
	}
}

abstract class ConstantInfoRef(val classIndex: Int, val nameAndType: Int) : ConstantInfoBase()

class ConstantInfoMethodRef(classIndex: Int, nameAndType: Int) : ConstantInfoRef(classIndex, nameAndType) {
	override fun getType(): ConstantType {
		return ConstantType.METHOD_REF;
	}
}

class ConstantInfoFieldRef(classIndex: Int, nameAndType: Int) : ConstantInfoRef(classIndex, nameAndType) {
	override fun getType(): ConstantType {
		return ConstantType.FIELD_REF;
	}
}

class ConstantInfoInterfaceMethodRef(classIndex: Int, nameAndType: Int) : ConstantInfoRef(classIndex, nameAndType) {
	override fun getType(): ConstantType {
		return ConstantType.INTERFACE_METHOD_REF;
	}
}

class ConstantInfoString(val stringIndex: Int) : ConstantInfoBase() {

	override fun getType(): ConstantType {
		return ConstantType.STRING
	}
}

class ConstantInfoInt(val value: Int) : ConstantInfoBase() {

	override fun getType(): ConstantType {
		return ConstantType.INT
	}
}

class ConstantInfoFloat(val value: Float) : ConstantInfoBase() {

	override fun getType(): ConstantType {
		return ConstantType.FLOAT
	}
}

class ConstantInfoLong(val value: Long) : ConstantInfoBase() {

	override fun getType(): ConstantType {
		return ConstantType.LONG
	}
}

class ConstantInfoDouble(val value: Double) : ConstantInfoBase() {

	override fun getType(): ConstantType {
		return ConstantType.DOUBLE
	}
}

class ConstantInfoNameAndType(val nameIndex: Int, val descriptorIndex: Int) : ConstantInfoBase() {

	override fun getType(): ConstantType {
		return ConstantType.NAME_AND_TYPE
	}
}

class ConstantInfoUTF8(val value: String, val length: Int = value.length ) : ConstantInfoBase() {

	override fun getType(): ConstantType {
		return ConstantType.UTF8
	}
}