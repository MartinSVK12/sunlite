package sunsetsatellite.vm.runtime

import sunsetsatellite.vm.classfile.*
import sunsetsatellite.vm.descriptor.Descriptor
import sunsetsatellite.vm.descriptor.FieldDescriptorAnalyzer
import sunsetsatellite.vm.exceptions.*
import sunsetsatellite.vm.obj.*
import sunsetsatellite.vm.obj.NullRef.getRef
import sunsetsatellite.vm.type.*

class VirtualMachine(val mainClass: String) {
	val classLoader: VmClassLoader = VmClassLoader()
	val refResolver: VmRefResolver = VmRefResolver()
	private val classDefinitions: MutableMap<String, ClassFile> = mutableMapOf()
	val heap: MutableList<AnyVmType> = mutableListOf()
	val stringHeap: MutableMap<String,ClassInstance> = mutableMapOf()
	val methodArea: MutableList<SymbolicReference<*>> = mutableListOf()
	lateinit var mainThread: VmThread

	fun start(){
		init()
		mainThread.run()
	}

	fun init(){
		mainThread = VmThread("main")
		val obj = newObject(mainClass)
		val mainMethod =
			obj.thisClass.staticMethods.find { it.name == "main" }?.get() ?: throw VmClassNotFoundError("main")
		mainThread.invokeStaticLater(mainMethod,null)
	}

	fun loadClass(className: String): ClassRef {
		classAlreadyLoaded(className)?.let { return@loadClass it }
		if(VmRuntime.debug) println("Loading class: ${className}")
		val classFile = classLoader.loadClass(className)
		val classRef = ClassRef(classFile.getClassName(),classFile)
		classDefinitions[className] = classFile
		methodArea.add(classRef)
		return classRef
	}

	fun loadMethod(classRef: ClassRef, methodName: String, methodDescriptor: String): MethodRef {
		methodAlreadyLoaded(classRef,methodName,methodDescriptor)?.let { return@loadMethod it }
		if(VmRuntime.debug) println("Loading method: ${classRef.name}::${methodName}${methodDescriptor}")
		val methodRef = MethodRef(
			name = methodName,
			descriptor = methodDescriptor,
			classRef = classRef
		)
		methodArea.add(methodRef)
		return methodRef
	}

	fun loadField(classRef: ClassRef, fieldName: String, fieldDescriptor: String): FieldRef<*> {
		fieldAlreadyLoaded(classRef,fieldName,fieldDescriptor)?.let { return@loadField it }
		if(VmRuntime.debug) println("Loading field: ${classRef.name}::${fieldName}${fieldDescriptor}")
		val fieldRef = FieldRef<Any>(
			name = fieldName,
			descriptor = fieldDescriptor,
			classRef = classRef
		)
		methodArea.add(fieldRef)
		return fieldRef
	}

	fun loadMethods(classRef: ClassRef, classDef: ClassFile, static: Boolean): Array<MethodRef> {
		return classDef.methods.map {
			val nameAndType: Pair<String, String> = classDef.getMethodNameAndType(it)
			this.loadMethod(classRef,nameAndType.first,nameAndType.second)
		}.dropWhile { (classDef.getMethodInfo(it.name,it.descriptor).access_flags.STATIC) != static }.toTypedArray()
	}

	fun loadFields(classRef: ClassRef, classDef: ClassFile, static: Boolean): Array<FieldRef<*>> {
		return classDef.fields.map {
			val nameAndType: Pair<String, String> = classDef.getFieldNameAndType(it)
			this.loadField(classRef,nameAndType.first,nameAndType.second)
		}.dropWhile { (classDef.getFieldInfo(it.name,it.descriptor).access_flags.STATIC) != static }.toTypedArray()
	}

	fun classAlreadyLoaded(className: String): ClassRef? {
		return methodArea.find { it.getType() == VmSymbolRefTypes.CLASS && (it as ClassRef).name == className } as ClassRef?
	}

	fun methodAlreadyLoaded(classRef: ClassRef, methodName: String, methodDescriptor: String): MethodRef? {
		return methodArea.find { it.getType() == VmSymbolRefTypes.METHOD &&
				(it as MethodRef).name == methodName &&
				it.descriptor == methodDescriptor &&
				it.classRef == classRef
		} as MethodRef?
	}

	fun fieldAlreadyLoaded(classRef: ClassRef, fieldName: String, fieldDescriptor: String): FieldRef<*>? {
		return methodArea.find { it.getType() == VmSymbolRefTypes.FIELD &&
				(it as FieldRef<*>).name == fieldName &&
				it.descriptor == fieldDescriptor &&
				it.classRef == classRef
		} as FieldRef<*>?
	}

	fun getClassDef(className: String): ClassFile {
		return classDefinitions[className]?: throw VmClassNotFoundError(className)
	}

	fun newObject(className: String): ClassInstance {
		val classRef = loadClass(className)
		val classDef = getClassDef(className)
		if(classDef.access_flags.ABSTRACT){
			throw VmIllegalCallException("cannot create a new instance of abstract class '$className'")
		}
		val newClass = classRef.get()
		val obj = ClassInstance(
			thisClass = newClass,
			methods = loadMethods(classRef, classDef, false),
			fields = loadFields(classRef, classDef, false),
		)
		if(classDef.getSuperClassName() != "null"){
			obj.superClass = newObject(classDef.getSuperClassName())
		}

		heap.add(RefType(ObjRef(obj)))
		return obj;
	}

	fun newString(string: String): ClassInstance {
		if(VmRuntime.vm.stringHeap.contains(string)){
			return VmRuntime.vm.stringHeap[string]!!
		}
		val stringObj = VmRuntime.vm.newObject("base/String")
		val field = VmRuntime.vm.findFieldByName(stringObj, "value", "[C")
		val vmCharArray = ArrayRef(VmTypes.CHAR,string.length)
		for ((i, c) in string.toCharArray().withIndex()) {
			vmCharArray.getRef()[i] = CharType(c)
		}
		field.value = RefType(vmCharArray)
		VmRuntime.vm.stringHeap[string] = stringObj
		return stringObj
	}

	fun newThrowable(message: String?): ClassInstance {
		val eObj = VmRuntime.vm.newObject("base/Throwable")
		VmRuntime.vm.findFieldByName(eObj,"message","Lbase/String;").value = RefType(ObjRef(VmRuntime.vm.newString(message ?: "")))
		return eObj
	}

	fun findMethodByName(obj: ClassInstance, name: String, descriptor: String, methodClassName: String, strict: Boolean = false): ResolvedMethod {
		var method = obj.methods.find { it.name == name && it.descriptor == descriptor && if(strict) obj.thisClass.ref.name == methodClassName else true }?.get()
		if(method == null){
			if (obj.superClass != null) method = findMethodByName(obj.superClass!!, name, descriptor, methodClassName)
			else throw VmMethodNotFoundError(name)
		}
		return method
	}


	fun findFieldByName(obj: ClassInstance, name: String, descriptor: String): ResolvedField<*> {
		var field = obj.fields.find { it.name == name && it.descriptor == descriptor }?.get()
		if(field == null){
			if (obj.superClass != null) field = findFieldByName(obj.superClass!!, name, descriptor)
			else throw VmFieldNotFoundError(name)
		}
		return field
	}


	fun getStaticMethodFromIndex(constantPool: ConstantPool, constantPoolIndex: Int): ResolvedMethod {
		val methodInfo = constantPool[constantPoolIndex] as ConstantInfoMethodRef
		val nameAndTypeInfo = (constantPool[methodInfo.nameAndType] as ConstantInfoNameAndType)

		val methodClassName =
			(constantPool[(constantPool[methodInfo.classIndex] as ConstantInfoClass).nameIndex] as ConstantInfoUTF8).value
		val methodName = (constantPool[nameAndTypeInfo.nameIndex] as ConstantInfoUTF8).value
		val methodDescriptor = (constantPool[nameAndTypeInfo.descriptorIndex] as ConstantInfoUTF8).value

		val method =
			this.loadClass(methodClassName).get().staticMethods.find { it.name == methodName && it.descriptor == methodDescriptor }
				?.get() ?: throw VmMethodNotFoundError(methodName)
		return method
	}

	fun checkCast(value: AnyVmType, desc: Descriptor){
		if (value.getType() != desc.type) {
			var stillInvalid = true
			if(value.getType() == VmTypes.REFERENCE){
				val refType = (value as RefType<*>).getTypeValue().getType()
				stillInvalid = refType.name != desc.type.name
				/*if(refType == VmReferenceTypes.ARRAY){
					if(desc.ref != "*"){
						val arrayDesc = FieldDescriptorAnalyzer.analyze(desc.ref)
						if(arrayDesc.type != desc.type){
							throw VmTypeCastException("cannot cast array of type '${arrayDesc.type.name.lowercase()}' to '${desc.type.name.lowercase()}'")
						}
					}
				}*/
			}
			if(stillInvalid) throw VmTypeCastException("cannot cast value of type '${value.getType().name.lowercase()}' to '${desc.type.name.lowercase()}'")
		} else if(value.getType() == VmTypes.REFERENCE && desc.type == VmTypes.REFERENCE) {
			val classInstance = (value as RefType<ObjRef>).getTypeValue().getRef()
			if(!isSuperClass(desc.ref,classInstance) && desc.ref != classInstance.thisClass.ref.name)
				throw VmTypeCastException("cannot cast reference of type '${classInstance.thisClass.ref.name}' to '${desc.ref}'")
		}
	}

	fun checkCast(desc: Descriptor, checkDesc: Descriptor){

	}

	fun isSuperClass(className: String, obj: ClassInstance): Boolean {
		if(obj.superClass == null) return false
		else if (obj.superClass!!.thisClass.ref.name == className) return true
		else return isSuperClass(className, obj.superClass!!)
	}

	fun isSuperOrThisClass(className: String, obj: ClassInstance): Boolean {
		if (obj.thisClass.ref.name == className) return true
		else return isSuperClass(className, obj)
	}

	fun getInstanceMethodFromIndex(obj: ClassInstance, constantPool: ConstantPool, constantPoolIndex: Int, strict: Boolean = false): ResolvedMethod {
		val methodInfo = constantPool[constantPoolIndex] as ConstantInfoMethodRef
		val nameAndTypeInfo = (constantPool[methodInfo.nameAndType] as ConstantInfoNameAndType)

		val methodClassName =
			(constantPool[(constantPool[methodInfo.classIndex] as ConstantInfoClass).nameIndex] as ConstantInfoUTF8).value
		val methodName = (constantPool[nameAndTypeInfo.nameIndex] as ConstantInfoUTF8).value
		val methodDescriptor = (constantPool[nameAndTypeInfo.descriptorIndex] as ConstantInfoUTF8).value

		if(strict) {
			if(!isSuperClass(methodClassName,obj) && methodClassName != obj.thisClass.ref.name){
				throw VmTypeCastException("reference of type '${obj.thisClass.ref.name}' does not inherit from type '${methodClassName}'")
			}
		}

		val method = findMethodByName(obj, methodName, methodDescriptor, methodClassName, strict)
		return method
	}

	fun getStaticFieldFromIndex(constantPool: ConstantPool, constantPoolIndex: Int): ResolvedField<out Any?> {
		val fieldInfo = constantPool[constantPoolIndex] as ConstantInfoFieldRef
		val nameAndTypeInfo = (constantPool[fieldInfo.nameAndType] as ConstantInfoNameAndType)

		val fieldClassName =
			(constantPool[(constantPool[fieldInfo.classIndex] as ConstantInfoClass).nameIndex] as ConstantInfoUTF8).value
		val fieldName = (constantPool[nameAndTypeInfo.nameIndex] as ConstantInfoUTF8).value
		val fieldDescriptor = (constantPool[nameAndTypeInfo.descriptorIndex] as ConstantInfoUTF8).value

		val field =
			this.loadClass(fieldClassName).get().staticFields.find { it.name == fieldName && it.descriptor == fieldDescriptor }
				?.get() ?: throw VmFieldNotFoundError(fieldName)
		return field
	}

	fun getInstanceFieldByIndex(obj: ClassInstance, constantPool: ConstantPool, constantPoolIndex: Int): ResolvedField<out Any?> {
		val fieldInfo = constantPool[constantPoolIndex] as ConstantInfoFieldRef
		val nameAndTypeInfo = (constantPool[fieldInfo.nameAndType] as ConstantInfoNameAndType)

		val fieldClassName =
			(constantPool[(constantPool[fieldInfo.classIndex] as ConstantInfoClass).nameIndex] as ConstantInfoUTF8).value
		val fieldName = (constantPool[nameAndTypeInfo.nameIndex] as ConstantInfoUTF8).value
		val fieldDescriptor = (constantPool[nameAndTypeInfo.descriptorIndex] as ConstantInfoUTF8).value

		val field = findFieldByName(obj,fieldName,fieldDescriptor)
		return field
	}

	fun getClassNameByIndex(constantPool: ConstantPool, constantPoolIndex: Int): String {
		return constantPool[constantPoolIndex].let {
			constantPool[(it as ConstantInfoClass).nameIndex].let {
				(it as ConstantInfoUTF8).value
			}
		}
	}

	fun getStringValue(obj: ClassInstance): String {
		if(!isSuperOrThisClass("base/String",obj)){
			throw VmTypeCastException("cannot cast reference of type '${obj.thisClass.ref.name}' to 'base/String'")
		}
		val field = findFieldByName(obj, "value", "[C")
		val array = (field.value as RefType<ArrayRef>).getTypeValue().getRef()

		var str = ""
		array.mapIndexed{
			i: Int, c: VmType<*> -> (c as CharType).getTypeValue()
		}.forEach {
			str += it
		}
		return str
	}

	fun getThrowableMessage(obj: ClassInstance?): String {
		if(obj == null) return ""
		if(!isSuperOrThisClass("base/Throwable",obj)){
			throw VmTypeCastException("cannot cast reference of type '${obj.thisClass.ref.name}' to 'base/Throwable'")
		}
		val messageField = findFieldByName(obj,"message","Lbase/String;")
		val objRef = (messageField.value as RefType<ObjRef>).getTypeValue()
		//TODO: don't force init like this or maybe do idk
		return getStringValue(objRef.makeInitialized().getRef())
	}

}