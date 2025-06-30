package sunsetsatellite.vm.sunlite

import sunsetsatellite.lang.sunlite.PrimitiveType
import sunsetsatellite.lang.sunlite.Type
import java.io.File.separator
import kotlin.math.*
import kotlin.system.exitProcess

object Natives {

	fun registerNatives(vm: VM) {
		registerCore(vm)
		registerString(vm)
		registerMath(vm)
	}

	fun registerCore(vm: VM){
		vm.defineNative(object : SLNativeFunction("clock", Type.NUMBER,0) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				return SLNumber(System.currentTimeMillis().toDouble() / 1000)
			}
		})

		vm.defineNative(object : SLNativeFunction("print",Type.NIL,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val value = args[0]
				vm.sunlite.printInfo(if(value is SLString) value.value else value.toString())
				return SLNil
			}
		})

		vm.defineNative(object : SLNativeFunction("str",Type.STRING,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				return SLString(args[0].toString())
			}
		})

		vm.defineNative(object : SLNativeFunction("array",Type.Reference(PrimitiveType.ARRAY, "<array>",Type.NULLABLE_ANY),1) {
			override fun call(
				vm: VM,
				args: Array<AnySLValue>
			): AnySLValue {
				return SLArrayObj(SLArray((args[0] as SLNumber).value.toInt(),vm.sunlite))
			}
		})

		vm.defineNative(object : SLNativeFunction("arrayOf",Type.Reference(PrimitiveType.ARRAY, "<array>",Type.NULLABLE_ANY),-1) {
			override fun call(
				vm: VM,
				args: Array<AnySLValue>
			): AnySLValue {
				return SLArrayObj(SLArray(args.size,vm.sunlite).overwrite(args))
			}
		})

		vm.defineNative(object : SLNativeFunction("resize",Type.NIL,2) {
			override fun call(
				vm: VM,
				args: Array<AnySLValue>
			): AnySLValue {
				val array = args[1] as SLArrayObj
				val newSize = args[0] as SLNumber
				array.value.resize(newSize.value.toInt())
				return SLNil
			}
		})

		vm.defineNative(object : SLNativeFunction("sizeOf",Type.NUMBER,1) {
			override fun call(
				vm: VM,
				args: Array<AnySLValue>
			): AnySLValue {
				val array = args[0] as SLArrayObj
				return SLNumber(array.value.size.toDouble())
			}
		})

		vm.defineNative(object : SLNativeFunction("typeOf",Type.STRING,1) {
			override fun call(
				vm: VM,
				args: Array<AnySLValue>
			): AnySLValue {
				val type = Type.fromValue(args[0].value, vm.sunlite)
				return SLString(type.toString())
			}
		})

		vm.defineNative(object : SLNativeFunction("launchArgs",Type.ofArray(Type.STRING),0) {
			override fun call(
				vm: VM,
				args: Array<AnySLValue>
			): AnySLValue {
				return SLArrayObj(SLArray(vm.launchArgs.size, vm.sunlite).overwrite(vm.launchArgs.map { SLString(it) }.toTypedArray()))
			}
		})
	}

	fun registerString(vm: VM){
		vm.defineNative(object : SLNativeFunction("string#len",Type.NUMBER,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val s = (args[0] as SLString).value
				return SLNumber(s.length.toDouble())
			}
		})

		vm.defineNative(object : SLNativeFunction("string#reverse",Type.STRING,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val s = (args[0] as SLString).value
				return SLString(s.reversed())
			}
		})

		vm.defineNative(object : SLNativeFunction("string#sub",Type.STRING,3) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val s = (args[0] as SLString).value
				val from = (args[1] as SLNumber).value.toInt()
				val to = (args[2] as SLNumber).value.toInt()
				return SLString(s.substring(from,to))
			}
		})

		vm.defineNative(object : SLNativeFunction("string#repeat",Type.STRING,3) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val s = (args[0] as SLString).value
				val n = (args[1] as SLNumber).value.toInt()
				val separator = (args[2] as SLString).value
				return SLString(Array(n) { it -> s}.joinToString(separator))
			}
		})

		vm.defineNative(object : SLNativeFunction("string#format",Type.STRING,2) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val s = (args[0] as SLString).value
				val fmt = (args[1] as SLArrayObj).value.internal()
				return SLString(s.format(*fmt))
			}
		})
	}

	fun registerMath(vm: VM){
		vm.defineNative(object : SLNativeFunction("abs",Type.NUMBER,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLNumber(abs(number))
			}
		})

		vm.defineNative(object : SLNativeFunction("acos",Type.NUMBER,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLNumber(acos(number))
			}
		})

		vm.defineNative(object : SLNativeFunction("asin",Type.NUMBER,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLNumber(asin(number))
			}
		})

		vm.defineNative(object : SLNativeFunction("atan",Type.NUMBER,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLNumber(atan(number))
			}
		})

		vm.defineNative(object : SLNativeFunction("atan2",Type.NUMBER,2) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val y = (args[0] as SLNumber).value
				val x = (args[1] as SLNumber).value
				return SLNumber(atan2(y,x))
			}
		})

		vm.defineNative(object : SLNativeFunction("ceil",Type.NUMBER,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLNumber(ceil(number))
			}
		})

		vm.defineNative(object : SLNativeFunction("cos",Type.NUMBER,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLNumber(cos(number))
			}
		})

		vm.defineNative(object : SLNativeFunction("deg",Type.NUMBER,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLNumber(Math.toDegrees(number))
			}
		})

		vm.defineNative(object : SLNativeFunction("rad",Type.NUMBER,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLNumber(Math.toRadians(number))
			}
		})

		vm.defineNative(object : SLNativeFunction("exp",Type.NUMBER,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLNumber(exp(number))
			}
		})

		vm.defineNative(object : SLNativeFunction("floor",Type.NUMBER,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLNumber(floor(number))
			}
		})

		vm.defineNative(object : SLNativeFunction("fmod",Type.NUMBER,2) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val x = (args[0] as SLNumber).value
				val y = (args[1] as SLNumber).value
				return SLNumber(Math.floorMod(x.toInt(),y.toInt()).toDouble())
			}
		})

		vm.defineNative(object : SLNativeFunction("log",Type.NUMBER,2) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val x = (args[0] as SLNumber).value
				val base = (args[1] as SLNumber).value
				return SLNumber(log(x,base))
			}
		})

		vm.defineNative(object : SLNativeFunction("max",Type.NUMBER,2) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val x = (args[0] as SLNumber).value
				val y = (args[1] as SLNumber).value
				return SLNumber(max(x,y))
			}
		})

		vm.defineNative(object : SLNativeFunction("min",Type.NUMBER,2) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val x = (args[0] as SLNumber).value
				val y = (args[1] as SLNumber).value
				return SLNumber(min(x,y))
			}
		})

		vm.defineNative(object : SLNativeFunction("pow",Type.NUMBER,2) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val x = (args[0] as SLNumber).value
				val y = (args[1] as SLNumber).value
				return SLNumber(x.pow(y))
			}
		})

		vm.defineNative(object : SLNativeFunction("pi",Type.NUMBER,0) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				return SLNumber(Math.PI)
			}
		})

		vm.defineNative(object : SLNativeFunction("sin",Type.NUMBER,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLNumber(sin(number))
			}
		})

		vm.defineNative(object : SLNativeFunction("sqrt",Type.NUMBER,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLNumber(sqrt(number))
			}
		})

		vm.defineNative(object : SLNativeFunction("tan",Type.NUMBER,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLNumber(tan(number))
			}
		})

		vm.defineNative(object : SLNativeFunction("exit",Type.NUMBER,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				exitProcess(number.toInt())
			}
		})

		vm.defineNative(object : SLNativeFunction("cosh",Type.NUMBER,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLNumber(cosh(number))
			}
		})

		vm.defineNative(object : SLNativeFunction("sinh",Type.NUMBER,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLNumber(sinh(number))
			}
		})

		vm.defineNative(object : SLNativeFunction("tanh",Type.NUMBER,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLNumber(tanh(number))
			}
		})
	}

}