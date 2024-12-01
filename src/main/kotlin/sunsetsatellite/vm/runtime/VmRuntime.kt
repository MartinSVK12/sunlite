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

	fun testFib(){
		val nativeTimes: MutableList<Duration> = mutableListOf()
		val vmTimes: MutableList<Duration> = mutableListOf()
		NativeMethods.registerNatives()
		Builtins.constructBuiltins()
		val times = 1000
		for (i in 0 until times) {
			val nativeTime = measureTime {
				println("fib: ${fib(0,1,45)}")
			}
			println("Native execution took $nativeTime")
			nativeTimes.add(nativeTime)
			println()

			println("Starting virtual machine...")

			val time = measureTime {
				vm = VirtualMachine("base/Main")
				vm.start()
			}

			println("VM execution took $time")
			vmTimes.add(time)
			println("Shutting down virtual machine...")
			println()
		}
		println()
		val nativeAverage = nativeTimes.sumOf { it.toInt(DurationUnit.MICROSECONDS) } / nativeTimes.size.toDouble()
		val vmAverage = vmTimes.sumOf { it.toInt(DurationUnit.MICROSECONDS) } / vmTimes.size.toDouble()
		println("Average native time (over $times runs): ${nativeAverage}us")
		println("Average VM time (over $times runs): ${vmAverage}us")
		println("VM is %.2fx slower than native execution.".format(vmAverage/nativeAverage))
	}

	fun fib(v1: Int, v2: Int, max: Int): Int {
		if(max <= 0) return v1+v2
		println("fib: ${v1+v2}")
		return fib(v1+v2, v1, max-1)
	}
}