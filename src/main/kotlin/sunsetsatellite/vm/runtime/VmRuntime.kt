package sunsetsatellite.vm.runtime

import sunsetsatellite.vm.classfile.ClassFile
import sunsetsatellite.vm.classfile.ConstantInfoBase
import sunsetsatellite.vm.code.InstructionSet
import sunsetsatellite.vm.method.NativeMethod
import sunsetsatellite.vm.type.VmType
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.measureTime

typealias AnyVmType = VmType<*>
typealias ConstantPool = Array<ConstantInfoBase>

object VmRuntime {
	lateinit var vm: VirtualMachine

	val debug: Boolean = false
	val maxFrameStackSize: Int = 1000

	val builtin: MutableMap<String, ClassFile> = mutableMapOf()
	val nativeMethods: MutableMap<String, NativeMethod> = mutableMapOf()

	init {
		Builtins.constructBuiltins()
	}

	@JvmStatic
	fun main(args: Array<String>) {
		NativeMethods.registerNatives()
		createAndStartVM(args)
	}

	fun createAndStartVM(args: Array<String>){
		println("Starting virtual machine with main class: ${args[0]}...")
		println("${InstructionSet.entries.size} instructions.")

		val time = measureTime {
			vm = VirtualMachine(args[0])
			vm.start()
		}

		println("VM run time: $time")
		println("Shutting down virtual machine...")
	}

	fun createVM(args: Array<String>){
		vm = VirtualMachine(args[0])
		vm.init()
	}
}