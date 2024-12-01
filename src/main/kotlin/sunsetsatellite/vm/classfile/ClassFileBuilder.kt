package sunsetsatellite.vm.classfile

import sunsetsatellite.vm.code.debug
import sunsetsatellite.vm.field.FieldAccessFlags
import sunsetsatellite.vm.field.FieldInfo
import sunsetsatellite.vm.method.MethodAccessFlags
import sunsetsatellite.vm.method.MethodInfo

class ClassFileBuilder() {

	private var major_version: Int = 0
	private var minor_version: Int = 0
	private var access_flags: ClassAccessFlags = ClassAccessFlags()
	private var this_class: Int = 0
	private var super_class: Int = -1
	private var constant_pool: MutableList<ConstantInfoBase> = mutableListOf()
	private var interfaces: MutableList<Int> = mutableListOf()
	private var fields: MutableList<FieldInfo> = mutableListOf()
	private var methods: MutableList<MethodInfo> = mutableListOf()
	private var attributes: MutableList<BaseAttribute> = mutableListOf()

	var methodToIndexMap: MutableMap<String, Int> = mutableMapOf()
	var fieldToIndexMap: MutableMap<String, Int> = mutableMapOf()
	var classToIndexMap: MutableMap<String, Int> = mutableMapOf()

	fun majorVersion(version: Int): ClassFileBuilder {
		this.major_version = version
		return this
	}

	fun minorVersion(version: Int): ClassFileBuilder {
		this.minor_version = version
		return this
	}

	fun accessFlags(flags: ClassAccessFlags): ClassFileBuilder {
		this.access_flags = flags
		return this
	}

	fun thisClass(s: String): ClassFileBuilder {
		this_class = addConstantPoolEntry(
			ConstantInfoClass(
				addConstantPoolEntry(
					ConstantInfoUTF8(s)
				)
			)
		)
		return this
	}

	fun superClass(s: String): ClassFileBuilder {
		super_class = addConstantPoolEntry(
			ConstantInfoClass(
				addConstantPoolEntry(
					ConstantInfoUTF8(s)
				)
			)
		)
		return this
	}

	fun addConstantPoolEntry(constantInfo: ConstantInfoBase): Int {
		this.constant_pool.add(constantInfo)
		return constant_pool.size-1
	}

	fun addMethodConstantEntry(name: String, descriptor: String, classConstantEntry: Int): Int {
		if(methodToIndexMap.containsKey("$name$descriptor@$classConstantEntry")) {
			return methodToIndexMap["$name$descriptor@$classConstantEntry"]!!
		}
		val nameTypeIndex = addConstantPoolEntry(
			ConstantInfoNameAndType(
				addConstantPoolEntry(ConstantInfoUTF8(name)),
				addConstantPoolEntry(ConstantInfoUTF8(descriptor)),
			)
		)
		val index = addConstantPoolEntry(ConstantInfoMethodRef(classConstantEntry, nameTypeIndex))
		methodToIndexMap["$name$descriptor@$classConstantEntry"] = index
		return index
	}

	fun addMethodConstantEntry(name: String, descriptor: String, className: String): Int {
		val classConstantEntry = addClassConstantEntry(className)
		if(methodToIndexMap.containsKey("$name$descriptor@$classConstantEntry")) {
			return methodToIndexMap["$name$descriptor@$classConstantEntry"]!!
		}
		val nameTypeIndex = addConstantPoolEntry(
			ConstantInfoNameAndType(
				addConstantPoolEntry(ConstantInfoUTF8(name)),
				addConstantPoolEntry(ConstantInfoUTF8(descriptor)),
			)
		)
		val index = addConstantPoolEntry(ConstantInfoMethodRef(classConstantEntry, nameTypeIndex))
		methodToIndexMap["$name$descriptor@$classConstantEntry"] = index
		return index
	}

	fun addFieldConstantEntry(name: String, descriptor: String, classConstantEntry: Int): Int {
		if(fieldToIndexMap.containsKey("$name$descriptor@$classConstantEntry")) {
			return fieldToIndexMap["$name$descriptor@$classConstantEntry"]!!
		}
		val nameTypeIndex = addConstantPoolEntry(
			ConstantInfoNameAndType(
				addConstantPoolEntry(ConstantInfoUTF8(name)),
				addConstantPoolEntry(ConstantInfoUTF8(descriptor)),
			)
		)
		val index = addConstantPoolEntry(ConstantInfoFieldRef(classConstantEntry, nameTypeIndex))
		fieldToIndexMap["$name$descriptor@$classConstantEntry"] = index
		return index
	}

	fun addFieldConstantEntry(name: String, descriptor: String, className: String): Int {
		val classConstantEntry = addClassConstantEntry(className)
		if(fieldToIndexMap.containsKey("$name$descriptor@$classConstantEntry")) {
			return fieldToIndexMap["$name$descriptor@$classConstantEntry"]!!
		}
		val nameTypeIndex = addConstantPoolEntry(
			ConstantInfoNameAndType(
				addConstantPoolEntry(ConstantInfoUTF8(name)),
				addConstantPoolEntry(ConstantInfoUTF8(descriptor)),
			)
		)
		val index = addConstantPoolEntry(ConstantInfoFieldRef(classConstantEntry, nameTypeIndex))
		fieldToIndexMap["$name$descriptor@$classConstantEntry"] = index
		return index
	}

	fun addClassConstantEntry(name: String): Int {
		if(classToIndexMap.containsKey(name)) {
			return classToIndexMap[name]!!
		}
		val index: Int = addConstantPoolEntry(
			ConstantInfoClass(
				addConstantPoolEntry(
					ConstantInfoUTF8(name)
				)
			)
		)
		classToIndexMap[name] = index
		return index
	}

	fun addStringConstantEntry(value: String): Int{
		return addConstantPoolEntry(ConstantInfoString(addConstantPoolEntry(ConstantInfoUTF8(value))))
	}

	fun addInterface(interfaceIndex: Int): ClassFileBuilder {
		//this.interfaces.add(interfaceIndex)
		return this
	}

	fun addField(name: String, descriptor: String, accessFlags: FieldAccessFlags, attributes: List<BaseAttribute>): ClassFileBuilder {
		val nameTypeIndex = addConstantPoolEntry(
			ConstantInfoNameAndType(
				addConstantPoolEntry(ConstantInfoUTF8(name)),
				addConstantPoolEntry(ConstantInfoUTF8(descriptor)),
			)
		)
		fieldToIndexMap["$name$descriptor"] = addConstantPoolEntry(ConstantInfoFieldRef(this_class,nameTypeIndex))
		this.fields.add(
			FieldInfo(
				access_flags = accessFlags,
				name_and_type_index = nameTypeIndex,
				class_index = this_class,
				attributes_count = attributes.size,
				attributes = attributes.toTypedArray()
			)
		)
		return this
	}

	fun addMethod(name: String, descriptor: String, accessFlags: MethodAccessFlags, attributes: List<BaseAttribute>): ClassFileBuilder {
		val nameTypeIndex = addConstantPoolEntry(
			ConstantInfoNameAndType(
				addConstantPoolEntry(ConstantInfoUTF8(name)),
				addConstantPoolEntry(ConstantInfoUTF8(descriptor)),
			)
		)
		methodToIndexMap["$name$descriptor"] = addConstantPoolEntry(ConstantInfoMethodRef(this_class,nameTypeIndex))
		this.methods.add(
			MethodInfo(
				access_flags = accessFlags,
				name_and_type_index = nameTypeIndex,
				class_index = this_class,
				attributes_count = attributes.size,
				attributes = attributes.toTypedArray()
			)
		)
		return this
	}

	fun addEmptyStaticInitMethod(): ClassFileBuilder {
		return addMethod(
			name = "<clinit>",
			descriptor = "()V",
			accessFlags = MethodAccessFlags().apply { PUBLIC = true; STATIC = true },
			attributes = listOf(
				CodeAttribute(0,
					arrayOf(
					)
				)
			)
		)
	}

	fun addEmptyInstanceInitMethod(): ClassFileBuilder {
		return addMethod(
			name = "<init>",
			descriptor = "()V",
			accessFlags = MethodAccessFlags().apply { PUBLIC = true },
			attributes = listOf(
				CodeAttribute(0,
					arrayOf(
					)
				)
			)
		)
	}

	fun addInstanceInitMethod(descriptor: String, accessFlags: MethodAccessFlags, attributes: List<BaseAttribute>): ClassFileBuilder {
		if(accessFlags.STATIC){
			throw IllegalArgumentException("instance init cannot be static")
		}
		return addMethod(
			name = "<init>",
			descriptor = descriptor,
			accessFlags = accessFlags,
			attributes = attributes
		)
	}

	fun addAttribute(attribute: BaseAttribute): ClassFileBuilder {
		this.attributes.add(attribute)
		return this
	}

	fun getThisClassIndex(): Int {
		return this_class
	}

	fun build(): ClassFile {
		return ClassFile(
			major_version,
			minor_version,
			constant_pool.size,
			access_flags,
			this_class,
			super_class,
			interfaces.size,
			fields.size,
			methods.size,
			attributes.size,
			constant_pool.toTypedArray(),
			interfaces.toIntArray(),
			fields.toTypedArray(),
			methods.toTypedArray(),
			attributes.toTypedArray()
		)
	}
}
