package sunsetsatellite.vm.sunlite

import sunsetsatellite.lang.sunlite.Param
import sunsetsatellite.lang.sunlite.PrimitiveType
import sunsetsatellite.lang.sunlite.Token
import sunsetsatellite.lang.sunlite.Type
import kotlin.math.*
import kotlin.random.Random
import kotlin.system.exitProcess

object DefaultNatives: Natives {

	override fun registerNatives(vm: VM) {
		registerCore(vm)
		registerIO(vm)
		registerString(vm)
		registerMath(vm)
		registerReflect(vm)
	}

	fun registerIO(vm: VM){
		vm.defineNative(object : SLNativeFunction("print",Type.NIL,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val value = args[0]
				vm.sunlite.printInfo(if(value is SLString) value.value else value.toString())
				return SLNil
			}
		})
	}

	fun registerReflect(vm: VM){
		vm.defineNative(object : SLNativeFunction("reflect#getMethods", Type.ofArray(Type.STRING),1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val clazz = args[0] as SLClassObj
				val array: Array<AnySLValue> = clazz.value.methods.keys.map { SLString(it) }.toTypedArray()
				return SLArrayObj(SLArray(array.size,vm.sunlite).overwrite(array))
			}
		})
		vm.defineNative(object : SLNativeFunction("reflect#getFields", Type.ofArray(Type.STRING),1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val clazz = args[0] as SLClassObj
				val array: Array<AnySLValue> = clazz.value.fieldDefaults.keys.map { SLString(it) }.toTypedArray()
				return SLArrayObj(SLArray(array.size,vm.sunlite).overwrite(array))
			}
		})
	}

	fun registerCore(vm: VM){
		vm.defineNative(object : SLNativeFunction("clock", Type.DOUBLE,0) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				return SLDouble(System.currentTimeMillis().toDouble() / 1000)
			}
		})

		vm.defineNative(object : SLNativeFunction("str",Type.STRING,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				return SLString(args[0].toString())
			}
		})

		vm.defineNative(object : SLNativeFunction("rand",Type.INT,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLInt(Random.nextInt(number.toInt()))
			}
		})


		vm.defineNative(object : SLNativeFunction("emptyArray",Type.ofArray(Type.NULLABLE_ANY),1) {
			override fun call(
				vm: VM,
				args: Array<AnySLValue>
			): AnySLValue {
				return SLArrayObj(SLArray((args[0] as SLNumber).value.toInt(),vm.sunlite))
			}
		})

		vm.defineNative(object : SLNativeFunction("emptyTable",Type.ofTable(Type.NULLABLE_ANY,Type.NULLABLE_ANY),0) {
			override fun call(
				vm: VM,
				args: Array<AnySLValue>
			): AnySLValue {
				return SLTableObj(SLTable(vm.sunlite))
			}
		})

		vm.defineNative(object : SLNativeFunction("arrayOf",Type.ofArray(Type.NULLABLE_ANY),-1) {
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

		vm.defineNative(object : SLNativeFunction("sizeOf",Type.INT,1) {
			override fun call(
				vm: VM,
				args: Array<AnySLValue>
			): AnySLValue {
				val array = args[0] as SLArrayObj
				return SLInt(array.value.size)
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

		vm.defineNative(object : SLNativeFunction("cls",Type.CLASS,1) {
			override fun call(
				vm: VM,
				args: Array<AnySLValue>
			): AnySLValue {
				val obj = args[0] as SLClassInstanceObj
				return SLClassObj(obj.value.clazz)
			}
		})

		vm.defineNative(object : SLNativeFunction("load",Type.ofFunction("",Type.NIL, listOf()),1){
			override fun call(
				vm: VM,
				args: Array<AnySLValue>
			): AnySLValue {
				val code = args[0] as SLString
				return vm.load(code.value) ?: SLNil
			}
		})
	}

	fun registerString(vm: VM){
		vm.defineNative(object : SLNativeFunction("string#len",Type.INT,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val s = (args[0] as SLString).value
				return SLInt(s.length)
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
				return SLString(Array(n) { s }.joinToString(separator))
			}
		})

		vm.defineNative(object : SLNativeFunction("string#format",Type.STRING,2) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val s = (args[0] as SLString).value
				val fmt = (args[1] as SLArrayObj).value.internal()
				return SLString(s.format(*fmt))
			}
		})

		vm.defineNative(object : SLNativeFunction("string#contains",Type.BOOLEAN,2) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val s = (args[0] as SLString).value
				val s2 = (args[1] as SLString).value
				return SLBool.of(s.contains(s2))
			}
		})
	}

	fun registerMath(vm: VM){
		vm.defineNative(object : SLNativeFunction("abs",Type.DOUBLE,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLDouble(abs(number.toDouble()))
			}
		})

		vm.defineNative(object : SLNativeFunction("acos",Type.DOUBLE,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLDouble(acos(number.toDouble()))
			}
		})

		vm.defineNative(object : SLNativeFunction("asin",Type.DOUBLE,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLDouble(asin(number.toDouble()))
			}
		})

		vm.defineNative(object : SLNativeFunction("atan",Type.DOUBLE,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLDouble(atan(number.toDouble()))
			}
		})

		vm.defineNative(object : SLNativeFunction("atan2",Type.DOUBLE,2) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val y = (args[0] as SLNumber).value
				val x = (args[1] as SLNumber).value
				return SLDouble(atan2(y.toDouble(),x.toDouble()))
			}
		})

		vm.defineNative(object : SLNativeFunction("ceil",Type.DOUBLE,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLDouble(ceil(number.toDouble()))
			}
		})

		vm.defineNative(object : SLNativeFunction("cos",Type.DOUBLE,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLDouble(cos(number.toDouble()))
			}
		})

		vm.defineNative(object : SLNativeFunction("deg",Type.DOUBLE,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLDouble(Math.toDegrees(number.toDouble()))
			}
		})

		vm.defineNative(object : SLNativeFunction("rad",Type.DOUBLE,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLDouble(Math.toRadians(number.toDouble()))
			}
		})

		vm.defineNative(object : SLNativeFunction("exp",Type.DOUBLE,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLDouble(exp(number.toDouble()))
			}
		})

		vm.defineNative(object : SLNativeFunction("floor",Type.DOUBLE,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLDouble(floor(number.toDouble()))
			}
		})

		vm.defineNative(object : SLNativeFunction("fmod",Type.DOUBLE,2) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val x = (args[0] as SLNumber).value
				val y = (args[1] as SLNumber).value
				return SLDouble(Math.floorMod(x.toInt(),y.toInt()).toDouble())
			}
		})

		vm.defineNative(object : SLNativeFunction("log",Type.DOUBLE,2) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val x = (args[0] as SLNumber).value
				val base = (args[1] as SLNumber).value
				return SLDouble(log(x.toDouble(),base.toDouble()))
			}
		})

		vm.defineNative(object : SLNativeFunction("max",Type.DOUBLE,2) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val x = (args[0] as SLNumber).value
				val y = (args[1] as SLNumber).value
				return SLDouble(max(x.toDouble(),y.toDouble()))
			}
		})

		vm.defineNative(object : SLNativeFunction("min",Type.DOUBLE,2) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val x = (args[0] as SLNumber).value
				val y = (args[1] as SLNumber).value
				return SLDouble(min(x.toDouble(),y.toDouble()))
			}
		})

		vm.defineNative(object : SLNativeFunction("pow",Type.DOUBLE,2) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val x = (args[0] as SLNumber).value
				val y = (args[1] as SLNumber).value
				return SLDouble(x.toDouble().pow(y.toDouble()))
			}
		})

		vm.defineNative(object : SLNativeFunction("pi",Type.DOUBLE,0) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				return SLDouble(Math.PI)
			}
		})

		vm.defineNative(object : SLNativeFunction("sin",Type.DOUBLE,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLDouble(sin(number.toDouble()))
			}
		})

		vm.defineNative(object : SLNativeFunction("sqrt",Type.DOUBLE,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLDouble(sqrt(number.toDouble()))
			}
		})

		vm.defineNative(object : SLNativeFunction("tan",Type.DOUBLE,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLDouble(tan(number.toDouble()))
			}
		})

		vm.defineNative(object : SLNativeFunction("exit",Type.DOUBLE,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				exitProcess(number.toInt())
			}
		})

		vm.defineNative(object : SLNativeFunction("cosh",Type.DOUBLE,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLDouble(cosh(number.toDouble()))
			}
		})

		vm.defineNative(object : SLNativeFunction("sinh",Type.DOUBLE,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLDouble(sinh(number.toDouble()))
			}
		})

		vm.defineNative(object : SLNativeFunction("tanh",Type.DOUBLE,1) {
			override fun call(vm: VM, args: Array<AnySLValue>): AnySLValue {
				val number = (args[0] as SLNumber).value
				return SLDouble(tanh(number.toDouble()))
			}
		})
	}

}