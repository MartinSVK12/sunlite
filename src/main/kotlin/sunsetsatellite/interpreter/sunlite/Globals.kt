package sunsetsatellite.interpreter.sunlite

import sunsetsatellite.lang.sunlite.FunctionModifier
import sunsetsatellite.lang.sunlite.Token
import sunsetsatellite.lang.sunlite.Type
import kotlin.system.exitProcess

object Globals {
    fun registerGlobals(globals: Environment) {
        globals.define("clock", object : LoxCallable {
            override fun call(interpreter: Interpreter, arguments: List<Any?>?, typeArguments: List<Type>): Any {
                return System.currentTimeMillis().toDouble() / 1000.0
            }

            override fun arity(): Int {
                return 0
            }

            override fun typeArity(): Int {
                return 0
            }

            override fun signature(): String {
                return "clock(): number"
            }

            override fun varargs(): Boolean {
                return false
            }

            override fun toString(): String {
                return "<global fn 'clock'>"
            }
        })

        globals.define("exit", object : LoxCallable {
            override fun call(interpreter: Interpreter, arguments: List<Any?>?, typeArguments: List<Type>): Any? {
                exitProcess(((arguments?.get(0) ?: 1) as Double).toInt())
            }

            override fun arity(): Int {
                return 1
            }

            override fun typeArity(): Int {
                return 0
            }

            override fun signature(): String {
                return "exit()"
            }

            override fun varargs(): Boolean {
                return false
            }

            override fun toString(): String {
                return "<global fn 'exit'>"
            }

        })

        globals.define("printerr", object : LoxCallable {
            override fun call(interpreter: Interpreter, arguments: List<Any?>?, typeArguments: List<Type>): Any? {
                System.err.println(arguments?.get(0) ?: "nil")
                return null
            }

            override fun arity(): Int {
                return 1
            }

            override fun typeArity(): Int {
                return 0
            }

            override fun signature(): String {
                return "printerr(o: any|nil)"
            }

            override fun varargs(): Boolean {
                return false
            }

            override fun toString(): String {
                return "<global fn 'printerr'>"
            }
        })

        globals.define("print", object : LoxCallable {
            override fun call(interpreter: Interpreter, arguments: List<Any?>?, typeArguments: List<Type>): Any? {
                println(arguments?.get(0) ?: "nil")
                return null
            }

            override fun arity(): Int {
                return 1
            }

            override fun typeArity(): Int {
                return 0
            }

            override fun signature(): String {
                return "print(o: any|nil)"
            }

            override fun varargs(): Boolean {
                return false
            }

            override fun toString(): String {
                return "<global fn 'print'>"
            }
        })

        globals.define("str", object : LoxCallable {
            override fun call(interpreter: Interpreter, arguments: List<Any?>?, typeArguments: List<Type>): Any {
                return arguments?.get(0).toString()
            }

            override fun arity(): Int {
                return 1
            }

            override fun typeArity(): Int {
                return 0
            }

            override fun signature(): String {
                return "str(o: any|nil): string"
            }

            override fun varargs(): Boolean {
                return false
            }

            override fun toString(): String {
                return "<global fn 'str'>"
            }

        })

        globals.define("num", object : LoxCallable {
            override fun call(interpreter: Interpreter, arguments: List<Any?>?, typeArguments: List<Type>): Any {
                try {
                    return arguments?.get(0).toString().toDouble()
                } catch (e: NumberFormatException) {
                    return 0
                }
            }

            override fun arity(): Int {
                return 1
            }

            override fun typeArity(): Int {
                return 0
            }

            override fun signature(): String {
                return "num(s: string): number"
            }

            override fun varargs(): Boolean {
                return false
            }

            override fun toString(): String {
                return "<global fn 'num'>"
            }

        })

        globals.define("stacktrace", object : LoxCallable {
            override fun call(interpreter: Interpreter, arguments: List<Any?>?, typeArguments: List<Type>): Any? {
                var env: Environment? = interpreter.environment
                System.err.println("sunlite stacktrace:")
                while (env != null) {
                    System.err.println("\tat ${env}")
                    env = env.enclosing
                }
                System.err.println()
                return null
            }

            override fun arity(): Int {
                return 0
            }

            override fun typeArity(): Int {
                return 0
            }

            override fun signature(): String {
                return "stacktrace()"
            }

            override fun varargs(): Boolean {
                return false
            }

            override fun toString(): String {
                return "<global fn 'stacktrace'>"
            }

        })

        globals.define("getmethods", object : LoxCallable {
            override fun call(interpreter: Interpreter, arguments: List<Any?>?, typeArguments: List<Type>): Any? {
                interpreter.sunlite.typeChecker.checkType(
                    Type.Union(listOf(Type.CLASS, Type.OBJECT)),
                    Type.fromValue(arguments?.get(0), interpreter.sunlite),
                    Token.identifier("getmethods", interpreter.environment.line, interpreter.environment.file), true)
                val loxClass: LoxClassInstance = arguments?.get(0) as LoxClassInstance
                val keys: MutableList<Any?> = loxClass.clazz?.methods?.keys?.toMutableList() ?: (loxClass as LoxClass).methods.filter { it.value.declaration.modifier == FunctionModifier.STATIC }.keys.toMutableList()
                val func: LoxCallable = globals.get(
                    Token.identifier(
                        "arrayof",
                        interpreter.environment.line,
                        interpreter.environment.file
                    )
                ) as LoxCallable
                return func.call(interpreter, keys.apply { this.add(0, keys.size.toDouble()) }, listOf(Type.STRING))
            }

            override fun arity(): Int {
                return 1
            }

            override fun typeArity(): Int {
                return 0
            }

            override fun signature(): String {
                return "getmethods(o: any): array<string>"
            }

            override fun varargs(): Boolean {
                return false
            }

            override fun toString(): String {
                return "<global fn 'getmethods'>"
            }

        })

        globals.define("getfields", object : LoxCallable {
            override fun call(interpreter: Interpreter, arguments: List<Any?>?, typeArguments: List<Type>): Any? {
                interpreter.sunlite.typeChecker.checkType(
                    Type.Union(listOf(Type.CLASS, Type.OBJECT)),
                    Type.fromValue(arguments?.get(0), interpreter.sunlite),
                    Token.identifier("getfields", interpreter.environment.line, interpreter.environment.file), true)
                val loxClass: LoxClassInstance = arguments?.get(0) as LoxClassInstance
                val keys: MutableList<Any?> = if(loxClass is LoxClass) loxClass.staticFields.keys.toMutableList() else loxClass.fields.keys.toMutableList()
                val func: LoxCallable = globals.get(
                    Token.identifier(
                        "arrayof",
                        interpreter.environment.line,
                        interpreter.environment.file
                    )
                ) as LoxCallable
                return func.call(interpreter, keys.apply { this.add(0, keys.size.toDouble()) }, listOf(Type.STRING))
            }

            override fun arity(): Int {
                return 1
            }

            override fun typeArity(): Int {
                return 0
            }

            override fun signature(): String {
                return "getfields(o: any): array<string>"
            }

            override fun varargs(): Boolean {
                return false
            }

            override fun toString(): String {
                return "<global fn 'getfields'>"
            }

        })

        globals.define("arrayof", object : LoxCallable {
            override fun call(interpreter: Interpreter, arguments: List<Any?>?, typeArguments: List<Type>): Any? {
                interpreter.sunlite.typeChecker.checkType(
                    Type.NUMBER,
                    Type.fromValue(arguments?.get(0), interpreter.sunlite),
                    Token.identifier("arrayof", interpreter.environment.line, interpreter.environment.file), true)
                val size: Int = (arguments?.get(0) as Double).toInt()
                val arr = LoxArray(typeArguments[0],size, interpreter.sunlite)
                arguments.forEachIndexed { index, element ->
                    if(index == 0) return@forEachIndexed
                    arr.set(index-1, element,
                        Token.identifier(
                            "<argument $index of global fn 'arrayof'>",
                            interpreter.environment.line,
                            interpreter.environment.file
                        )
                    )
                }
                return arr
            }

            override fun arity(): Int {
                return 1
            }

            override fun typeArity(): Int {
                return 1
            }

            override fun signature(): String {
                return "arrayof(<T> size: number, elements: <T>...): array<T>"
            }

            override fun varargs(): Boolean {
                return true
            }

            override fun toString(): String {
                return "<global fn 'arrayof'>"
            }

        })

        globals.define("size", object : LoxCallable {
            override fun call(interpreter: Interpreter, arguments: List<Any?>?, typeArguments: List<Type>): Any {
                interpreter.sunlite.typeChecker.checkType(
                    Type.Union(listOf(Type.ARRAY, Type.STRING)),
                    Type.fromValue(arguments?.get(0), interpreter.sunlite),
                    Token.identifier("size", interpreter.environment.line, interpreter.environment.file), true)
                if(arguments != null && arguments[0] != null){
                    if(arguments[0] is String){
                        return ((arguments[0] as String).length).toDouble()
                    } else if(arguments[0] is LoxArray){
                        return ((arguments[0] as LoxArray).size).toDouble()
                    }
                }
                return 0
            }

            override fun arity(): Int {
                return 1
            }

            override fun typeArity(): Int {
                return 0
            }

            override fun signature(): String {
                return "size(o: array|string): number"
            }

            override fun varargs(): Boolean {
                return false
            }

            override fun toString(): String {
                return "<global fn 'size'>"
            }

        })

        globals.define("resize", object : LoxCallable {
            override fun call(interpreter: Interpreter, arguments: List<Any?>?, typeArguments: List<Type>): Any? {
                interpreter.sunlite.typeChecker.checkType(
                    Type.ARRAY,
                    Type.fromValue(arguments?.get(0), interpreter.sunlite),
                    Token.identifier("resize", interpreter.environment.line, interpreter.environment.file), true)
                interpreter.sunlite.typeChecker.checkType(
                    Type.NUMBER,
                    Type.fromValue(arguments?.get(1), interpreter.sunlite),
                    Token.identifier("resize", interpreter.environment.line, interpreter.environment.file), true)
                val array = arguments?.get(0) as LoxArray
                array.resize((arguments[1] as Double).toInt())
                return null
            }

            override fun arity(): Int {
                return 2
            }

            override fun typeArity(): Int {
                return 0
            }

            override fun signature(): String {
                return "resize(arr: array, new_size: number)"
            }

            override fun varargs(): Boolean {
                return false
            }

            override fun toString(): String {
                return "<global fn 'resize'>"
            }

        })
    }


}