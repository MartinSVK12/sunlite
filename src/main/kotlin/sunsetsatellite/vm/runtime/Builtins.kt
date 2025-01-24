package sunsetsatellite.vm.runtime

import sunsetsatellite.vm.classfile.*
import sunsetsatellite.vm.code.*
import sunsetsatellite.vm.field.FieldAccessFlags
import sunsetsatellite.vm.method.MethodAccessFlags
import sunsetsatellite.vm.runtime.VmRuntime.builtin

object Builtins {
	fun constructBuiltins(){
		builtin.put("base/Object", constructObject())
		builtin.put("base/Main", constructMainClass())
		builtin.put("base/String", constructString())
		builtin.put("base/Throwable", constructThrowable())
		builtin.put("base/StdOut", constructStdoutClass())
	}

	fun constructObject(): ClassFile {
		val cfb = ClassFileBuilder()
		return cfb
			.majorVersion(1)
			.minorVersion(0)
			.thisClass("base/Object")
			.addEmptyStaticInitMethod()
			.addEmptyInstanceInitMethod()
			.build()
	}

	fun constructMainClass(): ClassFile {
		val cfb = ClassFileBuilder()
		return cfb
			.majorVersion(1)
			.minorVersion(0)
			.thisClass("base/Main")
			.superClass("base/Object")
			.addMethod(
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
			.addMethod(
				name = "main",
				descriptor = "()V",
				accessFlags = MethodAccessFlags().apply { PUBLIC = true; STATIC = true },
				attributes = listOf(
					CodeAttribute(2,
						arrayOf(
							ldc(cfb.addStringConstantEntry("hello lua from craftvm!")),
							dup(),
							invokeinit(),
							invokestatic(cfb.addMethodConstantEntry("println","(Lbase/String;)V","base/StdOut")),
							vreturn()
						)
					)
				)
			)
			.build()
	}

	fun constructThrowable(): ClassFile {
		val cfb = ClassFileBuilder()
		return cfb
			.majorVersion(1)
			.minorVersion(0)
			.thisClass("base/Throwable")
			.superClass("base/Object")
			.addEmptyStaticInitMethod()
			.addField(
				name = "message",
				descriptor = "Lbase/String;",
				accessFlags = FieldAccessFlags().apply { PUBLIC = true },
				attributes = listOf()
			)
			.addField(
				name = "cause",
				descriptor = "Lbase/Throwable;",
				accessFlags = FieldAccessFlags().apply { PUBLIC = true },
				attributes = listOf()
			)
			.build()
	}

	fun constructString(): ClassFile {
		val cfb = ClassFileBuilder()
		return cfb
			.majorVersion(1)
			.minorVersion(0)
			.thisClass("base/String")
			.superClass("base/Object")
			.addEmptyStaticInitMethod()
			.addField(
				name = "value",
				descriptor = "[C",
				accessFlags = FieldAccessFlags().apply { PUBLIC = true },
				attributes = listOf()
			)
			.build()
	}

	fun constructStdoutClass(): ClassFile {
		val cfb = ClassFileBuilder()
		return cfb
			.majorVersion(1)
			.minorVersion(0)
			.thisClass("base/StdOut")
			.superClass("base/Object")
			.addEmptyStaticInitMethod()
			.addMethod(
				name = "println",
				descriptor = "(Lbase/String;)V",
				accessFlags = MethodAccessFlags().apply { PUBLIC = true; STATIC = true; NATIVE = true },
				attributes = listOf()
			)
			.build()
	}

}