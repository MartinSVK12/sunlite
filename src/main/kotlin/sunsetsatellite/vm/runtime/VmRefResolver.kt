package sunsetsatellite.vm.runtime

import sunsetsatellite.vm.classfile.ClassFile
import sunsetsatellite.vm.classfile.CodeAttribute
import sunsetsatellite.vm.code.Code
import sunsetsatellite.vm.code.EmptyCode
import sunsetsatellite.vm.exceptions.VmResolutionError
import sunsetsatellite.vm.obj.*
import sunsetsatellite.vm.type.VmTypes

class VmRefResolver() {
	fun <T> resolveRef(symbolRef: SymbolicReference<T>): T {
		when(symbolRef){
			is ClassRef -> {
				if(VmRuntime.debug) println("Resolving symbolic class reference: ${symbolRef.name}")
				val classDef: ClassFile = VmRuntime.vm.getClassDef(symbolRef.name)
				val ref = ResolvedClass(
					ref = symbolRef,
					major = classDef.major_version,
					minor = classDef.minor_version,
					accessFlags = classDef.access_flags,
					superClass = if (classDef.super_class == -1) null else VmRuntime.vm.loadClass(classDef.getSuperClassName()),
					interfaces = arrayOf(),
					staticMethods = VmRuntime.vm.loadMethods(symbolRef,classDef,true),
					staticFields = VmRuntime.vm.loadFields(symbolRef, classDef,true),
				)
				VmRuntime.vm.mainThread.invokeStaticInit(ref, VmRuntime.vm.mainThread.frameStack.getOrNull(VmRuntime.vm.mainThread.frameStack.size-1))
				symbolRef.makeResolved()
				return ref as T
			}
			is MethodRef -> {
				if(VmRuntime.debug) println("Resolving symbolic method reference: ${symbolRef.name}${symbolRef.descriptor}")
				val classDef: ClassFile = VmRuntime.vm.getClassDef(symbolRef.classRef.name)
				val methodInfo = classDef.getMethodInfo(symbolRef.name,symbolRef.descriptor)
				val ref: ResolvedMethod
				if(methodInfo.attributes.isNotEmpty() && methodInfo.attributes[0] is CodeAttribute){
					val codeAttribute = methodInfo.attributes.get(0) as CodeAttribute
					ref = ResolvedMethod(
						ref = symbolRef,
						accessFlags = methodInfo.access_flags,
						code = Code(
							maxLocals = codeAttribute.maxLocals,
							maxStack = codeAttribute.maxStack,
							exceptionTable = arrayOf(),
							inst = codeAttribute.code
						)
					)
					symbolRef.makeResolved()
				} else {
					ref = ResolvedMethod(
						ref = symbolRef,
						accessFlags = methodInfo.access_flags,
						code = EmptyCode()
					)
					symbolRef.makeResolved()
				}

				return ref as T
			}
			is FieldRef<*> -> {
				if(VmRuntime.debug) println("Resolving symbolic field reference: ${symbolRef.name}:${symbolRef.descriptor}")
				val classDef: ClassFile = VmRuntime.vm.getClassDef(symbolRef.classRef.name)
				val type = VmTypes.getType(symbolRef.descriptor)
				val fieldInfo = classDef.getFieldInfo(symbolRef.name,symbolRef.descriptor)
				val accessFlags = fieldInfo.access_flags
				when (type) {
					VmTypes.INT -> {
						val ref = ResolvedIntField(
							ref = symbolRef as FieldRef<Int>,
							accessFlags = accessFlags
						)
						symbolRef.makeResolved()
						return ref as T
					}
					VmTypes.FLOAT -> {
						val ref = ResolvedFloatField(
							ref = symbolRef as FieldRef<Float>,
							accessFlags = accessFlags
						)
						symbolRef.makeResolved()
						return ref as T
					}
					VmTypes.LONG -> {
						val ref = ResolvedLongField(
							ref = symbolRef as FieldRef<Long>,
							accessFlags = accessFlags
						)
						symbolRef.makeResolved()
						return ref as T
					}
					VmTypes.DOUBLE -> {
						val ref = ResolvedDoubleField(
							ref = symbolRef as FieldRef<Double>,
							accessFlags = accessFlags
						)
						symbolRef.makeResolved()
						return ref as T
					}
					VmTypes.CHAR -> {
						val ref = ResolvedCharField(
							ref = symbolRef as FieldRef<Char>,
							accessFlags = accessFlags
						)
						symbolRef.makeResolved()
						return ref as T
					}
					VmTypes.BOOL -> {
						val ref = ResolvedBoolField(
							ref = symbolRef as FieldRef<Boolean>,
							accessFlags = accessFlags
						)
						symbolRef.makeResolved()
						return ref as T
					}
					VmTypes.REFERENCE -> {
						val ref = ResolvedRefField<Reference<*>>(
							ref = symbolRef as FieldRef<Reference<*>>,
							accessFlags = accessFlags
						)
						symbolRef.makeResolved()
						return ref as T
					}
					VmTypes.ARRAY -> {
						val ref = ResolvedRefField<Reference<*>>(
							ref = symbolRef as FieldRef<Reference<*>>,
							accessFlags = accessFlags
						)
						symbolRef.makeResolved()
						return ref as T
					}
					VmTypes.VOID -> throw VmResolutionError("field cannot be of void type")
					else -> throw VmResolutionError("unknown type")
				}
			}
		}
		throw VmResolutionError("unknown reference")
	}
}