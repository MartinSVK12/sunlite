package sunsetsatellite.sunlite.vm

import sunsetsatellite.sunlite.lang.Type
import java.io.File
import java.io.IOException
import kotlin.math.*
import kotlin.random.Random
import kotlin.system.exitProcess

object DefaultNatives : Natives {

    data object DefaultNativesContainer : NativesContainer {
        private val natives: MutableMap<String, AnySLValue> = mutableMapOf()

        init {
            registerNatives(this)
        }

        override fun defineNative(function: SLNativeFunction) {
            natives[function.name] = SLNativeFuncObj(function)
        }

        override fun getNatives(): Map<String, AnySLValue> {
            return natives
        }
    }

    override fun registerNatives(consumer: NativesContainer) {
        registerCore(consumer)
        registerIO(consumer)
        registerString(consumer)
        registerMath(consumer)
        registerReflect(consumer)
    }

    fun registerIO(natives: NativesContainer) {
        natives.defineNative(object : SLNativeFunction("print", Type.NIL, 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val value = args[0]
                vm.sunlite.printInfo(if (value is SLString) value.value else value.toString())
                return SLNil
            }
        })

        natives.defineNative(object : SLNativeFunction("File#openN", Type.ofObject("File"), 1) {
            override fun call(
                vm: VM,
                args: Array<AnySLValue>,
                typeArgs: Array<SLType>,
            ): AnySLValue {
                val slFile = (args[0] as SLClassInstanceObj).value
                vm.typeChecker.checkType(Type.ofObject("File"), Type.fromValue(slFile, vm.sunlite), true)
                val filename = (slFile.fields["filename"]?.value as SLString).value
                val file = File(filename)
                try {
                    file.createNewFile()
                    slFile.fields["<foreign>fileHandle"] = SLField(Type.UNKNOWN, SLForeignObject(file))
                    return args[0]
                } catch (e: IOException) {
                    vm.runtimeError(e.message ?: "null")
                    return SLNil
                }
            }
        })

        natives.defineNative(object : SLNativeFunction("File#readBytesN", Type.ofArray(Type.BYTE), 1) {
            override fun call(
                vm: VM,
                args: Array<AnySLValue>,
                typeArgs: Array<SLType>,
            ): AnySLValue {
                val file = ((args[0] as SLClassInstanceObj).value.fields["<foreign>fileHandle"]?.value as SLForeignObject).value as File
                file.readBytes().let { return SLArrayObj(SLArray(it.size, vm.sunlite, Type.BYTE).overwrite(it.map { SLByte(it) }.toTypedArray())) }
            }
        })

        natives.defineNative(object : SLNativeFunction("File#readTextN", Type.STRING, 1) {
            override fun call(
                vm: VM,
                args: Array<AnySLValue>,
                typeArgs: Array<SLType>,
            ): AnySLValue {
                val file = ((args[0] as SLClassInstanceObj).value.fields["<foreign>fileHandle"]?.value as SLForeignObject).value as File
                return SLString(file.readText())
            }
        })
    }

    fun registerReflect(natives: NativesContainer) {
        natives.defineNative(object : SLNativeFunction("Reflect#getMethods", Type.ofArray(Type.STRING), 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val clazz = args[0] as SLClassObj
                val array: Array<AnySLValue> = clazz.value.methods.keys.map { SLString(it) }.toTypedArray()
                return SLArrayObj(SLArray(array.size, vm.sunlite, Type.STRING).overwrite(array))
            }
        })
        natives.defineNative(object : SLNativeFunction("Reflect#getFields", Type.ofArray(Type.STRING), 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val clazz = args[0] as SLClassObj
                val array: Array<AnySLValue> = clazz.value.fieldDefaults.map { SLString("${it.key}: ${it.value.type}") }.toTypedArray()
                return SLArrayObj(SLArray(array.size, vm.sunlite, Type.STRING).overwrite(array))
            }
        })
    }

    fun registerCore(natives: NativesContainer) {
        natives.defineNative(object : SLNativeFunction("clock", Type.DOUBLE, 0) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                return SLDouble(System.currentTimeMillis().toDouble() / 1000)
            }
        })

        natives.defineNative(object : SLNativeFunction("str", Type.STRING, 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                return SLString(args[0].toString())
            }
        })

        natives.defineNative(object : SLNativeFunction("parseDouble", Type.DOUBLE, 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val s = (args[0] as SLString).value
                return SLDouble(s.toDouble())
            }
        })

        natives.defineNative(object : SLNativeFunction("parseInt", Type.INT, 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val s = (args[0] as SLString).value
                return SLInt(s.toInt())
            }
        })

        natives.defineNative(object : SLNativeFunction("ord", Type.SHORT, 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                return SLShort((args[0] as SLString).value.codePointAt(0).toShort())
            }
        })

        natives.defineNative(object : SLNativeFunction("chr", Type.STRING, 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                return SLString((args[0] as SLNumber).value.toInt().toChar().toString())
            }
        })

        natives.defineNative(object : SLNativeFunction("rand", Type.INT, 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val number = (args[0] as SLNumber).value
                return SLInt(Random.nextInt(number.toInt()))
            }
        })


        natives.defineNative(object : SLNativeFunction("emptyArray", Type.ofArray(Type.NULLABLE_ANY), 1, 1) {
            override fun call(
                vm: VM,
                args: Array<AnySLValue>,
                typeArgs: Array<SLType>,
            ): AnySLValue {
                return SLArrayObj(SLArray((args[0] as SLNumber).value.toInt(), vm.sunlite, typeArgs[0].value))
            }
        })

        natives.defineNative(object : SLNativeFunction("emptyTable", Type.ofTable(Type.NULLABLE_ANY, Type.NULLABLE_ANY), 0, 2) {
            override fun call(
                vm: VM,
                args: Array<AnySLValue>,
                typeArgs: Array<SLType>,
            ): AnySLValue {
                return SLTableObj(SLTable(vm.sunlite, typeArgs[0].value to typeArgs[1].value))
            }
        })

        natives.defineNative(object : SLNativeFunction("arrayOf", Type.ofArray(Type.NULLABLE_ANY), -1,1) {
            override fun call(
                vm: VM,
                args: Array<AnySLValue>,
                typeArgs: Array<SLType>,
            ): AnySLValue {
                return SLArrayObj(SLArray(args.size, vm.sunlite, typeArgs[0].value).overwrite(args))
            }
        })

        natives.defineNative(object : SLNativeFunction("resize", Type.NIL, 2) {
            override fun call(
                vm: VM,
                args: Array<AnySLValue>,
                typeArgs: Array<SLType>,
            ): AnySLValue {
                val array = args[0] as SLArrayObj
                val newSize = args[1] as SLNumber
                array.value.resize(newSize.value.toInt())
                return SLNil
            }
        })

        natives.defineNative(object : SLNativeFunction("sizeOf", Type.INT, 1) {
            override fun call(
                vm: VM,
                args: Array<AnySLValue>,
                typeArgs: Array<SLType>,
            ): AnySLValue {
                val array = args[0] as SLArrayObj
                return SLInt(array.value.size)
            }
        })

        natives.defineNative(object : SLNativeFunction("typeOf", Type.STRING, 1) {
            override fun call(
                vm: VM,
                args: Array<AnySLValue>,
                typeArgs: Array<SLType>,
            ): AnySLValue {
                val type = Type.fromValue(args[0].value, vm.sunlite)
                return SLString(type.toString())
            }
        })

        natives.defineNative(object : SLNativeFunction("launchArgs", Type.ofArray(Type.STRING), 0) {
            override fun call(
                vm: VM,
                args: Array<AnySLValue>,
                typeArgs: Array<SLType>,
            ): AnySLValue {
                return SLArrayObj(SLArray(vm.launchArgs.size, vm.sunlite, Type.STRING).overwrite(vm.launchArgs.map { SLString(it) }
                    .toTypedArray()))
            }
        })

        natives.defineNative(object : SLNativeFunction("cls", Type.CLASS, 1) {
            override fun call(
                vm: VM,
                args: Array<AnySLValue>,
                typeArgs: Array<SLType>,
            ): AnySLValue {
                val obj = args[0] as SLClassInstanceObj
                return SLClassObj(obj.value.clazz)
            }
        })

        natives.defineNative(object : SLNativeFunction("load", Type.ofFunction("", Type.NIL, listOf()), 1) {
            override fun call(
                vm: VM,
                args: Array<AnySLValue>,
                typeArgs: Array<SLType>,
            ): AnySLValue {
                val code = args[0] as SLString
                return vm.load(code.value) ?: SLNil
            }
        })

        natives.defineNative(object : SLNativeFunction("getStacktrace", Type.ofArray(Type.STRING), 1) {
            override fun call(
                vm: VM,
                args: Array<AnySLValue>,
                typeArgs: Array<SLType>,
            ): AnySLValue {
                val trace = vm.getCurrentStacktrace((args[0] as SLBool).value)
                return SLArrayObj(SLArray(trace.size, vm.sunlite, Type.STRING).overwrite(trace as Array<AnySLValue>))
            }
        })
    }

    fun registerString(natives: NativesContainer) {
        natives.defineNative(object : SLNativeFunction("string#len", Type.INT, 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val s = (args[0] as SLString).value
                return SLInt(s.length)
            }
        })

        natives.defineNative(object : SLNativeFunction("string#reverse", Type.STRING, 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val s = (args[0] as SLString).value
                return SLString(s.reversed())
            }
        })

        natives.defineNative(object : SLNativeFunction("string#sub", Type.STRING, 3) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val s = (args[0] as SLString).value
                val from = (args[1] as SLNumber).value.toInt()
                val to = (args[2] as SLNumber).value.toInt()
                return SLString(s.substring(from, to))
            }
        })

        natives.defineNative(object : SLNativeFunction("string#repeat", Type.STRING, 3) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val s = (args[0] as SLString).value
                val n = (args[1] as SLNumber).value.toInt()
                val separator = (args[2] as SLString).value
                return SLString(Array(n) { s }.joinToString(separator))
            }
        })

        natives.defineNative(object : SLNativeFunction("string#format", Type.STRING, 2) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val s = (args[0] as SLString).value
                val fmt = (args[1] as SLArrayObj).value.internal()
                return SLString(s.format(*fmt))
            }
        })

        natives.defineNative(object : SLNativeFunction("string#replace", Type.STRING, 3) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val s = (args[0] as SLString).value
                val replace = (args[0] as SLString).value
                val with = (args[0] as SLString).value
                return SLString(s.replace(replace, with))
            }
        })

        natives.defineNative(object : SLNativeFunction("string#trim", Type.STRING, 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val s = (args[0] as SLString).value
                return SLString(s.filterNot { it.isWhitespace() })
            }
        })

        natives.defineNative(object : SLNativeFunction("string#contains", Type.BOOLEAN, 2) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val s = (args[0] as SLString).value
                val s2 = (args[1] as SLString).value
                return SLBool.of(s.contains(s2))
            }
        })

        natives.defineNative(object : SLNativeFunction("string#at", Type.STRING, 2) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val s = (args[0] as SLString).value
                val index = (args[1] as SLInt).value
                return SLString(s[index].toString())
            }
        })
    }

    fun registerMath(natives: NativesContainer) {
        natives.defineNative(object : SLNativeFunction("abs", Type.DOUBLE, 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val number = (args[0] as SLNumber).value
                return SLDouble(abs(number.toDouble()))
            }
        })

        natives.defineNative(object : SLNativeFunction("acos", Type.DOUBLE, 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val number = (args[0] as SLNumber).value
                return SLDouble(acos(number.toDouble()))
            }
        })

        natives.defineNative(object : SLNativeFunction("asin", Type.DOUBLE, 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val number = (args[0] as SLNumber).value
                return SLDouble(asin(number.toDouble()))
            }
        })

        natives.defineNative(object : SLNativeFunction("atan", Type.DOUBLE, 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val number = (args[0] as SLNumber).value
                return SLDouble(atan(number.toDouble()))
            }
        })

        natives.defineNative(object : SLNativeFunction("atan2", Type.DOUBLE, 2) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val y = (args[0] as SLNumber).value
                val x = (args[1] as SLNumber).value
                return SLDouble(atan2(y.toDouble(), x.toDouble()))
            }
        })

        natives.defineNative(object : SLNativeFunction("ceil", Type.DOUBLE, 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val number = (args[0] as SLNumber).value
                return SLDouble(ceil(number.toDouble()))
            }
        })

        natives.defineNative(object : SLNativeFunction("cos", Type.DOUBLE, 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val number = (args[0] as SLNumber).value
                return SLDouble(cos(number.toDouble()))
            }
        })

        natives.defineNative(object : SLNativeFunction("deg", Type.DOUBLE, 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val number = (args[0] as SLNumber).value
                return SLDouble(Math.toDegrees(number.toDouble()))
            }
        })

        natives.defineNative(object : SLNativeFunction("rad", Type.DOUBLE, 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val number = (args[0] as SLNumber).value
                return SLDouble(Math.toRadians(number.toDouble()))
            }
        })

        natives.defineNative(object : SLNativeFunction("exp", Type.DOUBLE, 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val number = (args[0] as SLNumber).value
                return SLDouble(exp(number.toDouble()))
            }
        })

        natives.defineNative(object : SLNativeFunction("floor", Type.DOUBLE, 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val number = (args[0] as SLNumber).value
                return SLDouble(floor(number.toDouble()))
            }
        })

        natives.defineNative(object : SLNativeFunction("fmod", Type.DOUBLE, 2) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val x = (args[0] as SLNumber).value
                val y = (args[1] as SLNumber).value
                return SLDouble(Math.floorMod(x.toInt(), y.toInt()).toDouble())
            }
        })

        natives.defineNative(object : SLNativeFunction("log", Type.DOUBLE, 2) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val x = (args[0] as SLNumber).value
                val base = (args[1] as SLNumber).value
                return SLDouble(log(x.toDouble(), base.toDouble()))
            }
        })

        natives.defineNative(object : SLNativeFunction("max", Type.DOUBLE, 2) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val x = (args[0] as SLNumber).value
                val y = (args[1] as SLNumber).value
                return SLDouble(max(x.toDouble(), y.toDouble()))
            }
        })

        natives.defineNative(object : SLNativeFunction("min", Type.DOUBLE, 2) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val x = (args[0] as SLNumber).value
                val y = (args[1] as SLNumber).value
                return SLDouble(min(x.toDouble(), y.toDouble()))
            }
        })

        natives.defineNative(object : SLNativeFunction("pow", Type.DOUBLE, 2) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val x = (args[0] as SLNumber).value
                val y = (args[1] as SLNumber).value
                return SLDouble(x.toDouble().pow(y.toDouble()))
            }
        })

        natives.defineNative(object : SLNativeFunction("pi", Type.DOUBLE, 0) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                return SLDouble(Math.PI)
            }
        })

        natives.defineNative(object : SLNativeFunction("sin", Type.DOUBLE, 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val number = (args[0] as SLNumber).value
                return SLDouble(sin(number.toDouble()))
            }
        })

        natives.defineNative(object : SLNativeFunction("sqrt", Type.DOUBLE, 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val number = (args[0] as SLNumber).value
                return SLDouble(sqrt(number.toDouble()))
            }
        })

        natives.defineNative(object : SLNativeFunction("tan", Type.DOUBLE, 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val number = (args[0] as SLNumber).value
                return SLDouble(tan(number.toDouble()))
            }
        })

        natives.defineNative(object : SLNativeFunction("exit", Type.DOUBLE, 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val number = (args[0] as SLNumber).value
                exitProcess(number.toInt())
            }
        })

        natives.defineNative(object : SLNativeFunction("cosh", Type.DOUBLE, 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val number = (args[0] as SLNumber).value
                return SLDouble(cosh(number.toDouble()))
            }
        })

        natives.defineNative(object : SLNativeFunction("sinh", Type.DOUBLE, 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val number = (args[0] as SLNumber).value
                return SLDouble(sinh(number.toDouble()))
            }
        })

        natives.defineNative(object : SLNativeFunction("tanh", Type.DOUBLE, 1) {
            override fun call(vm: VM, args: Array<AnySLValue>, typeArgs: Array<SLType>): AnySLValue {
                val number = (args[0] as SLNumber).value
                return SLDouble(tanh(number.toDouble()))
            }
        })
    }

}