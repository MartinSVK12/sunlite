package sunsetsatellite.vm.classfile

import sunsetsatellite.vm.code.ExceptionTableEntry
import sunsetsatellite.vm.code.Instruction
import sunsetsatellite.vm.type.AttributeType

abstract class BaseAttribute(val nameIndex: Int, val length: Int) {

	abstract fun getType(): AttributeType
}

class ConstantValueAttribute(nameIndex: Int, val valueIndex: Int) : BaseAttribute(nameIndex, 2) {
	override fun getType(): AttributeType {
		return AttributeType.CONSTANT_VALUE
	}
};

class CodeAttribute(
	nameIndex: Int,
	length: Int,
	val maxStack: Int,
	val maxLocals: Int,
	val code: Array<Instruction>,
	val codeLength: Int,
	val exceptionTableLength: Int,
	val exceptionTable: Array<ExceptionTableEntry>,
	val attributeSize: Int,
	val attributes: Array<BaseAttribute>) : BaseAttribute(nameIndex, length)
{
		constructor(maxLocals: Int, code: Array<Instruction>) : this(0,0,0,maxLocals,code,code.size,0, arrayOf(),0, arrayOf())

	override fun getType(): AttributeType {
		return AttributeType.CODE
	}
}

class ExceptionsAttribute(nameIndex: Int, length: Int, val exceptionCount: Int, val exceptionIndexTable: IntArray) : BaseAttribute(nameIndex, length) {
	override fun getType(): AttributeType {
		return AttributeType.EXCEPTIONS
	}
}