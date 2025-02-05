package sunsetsatellite.lang.lox

import kotlin.system.exitProcess

object Globals {
    fun registerGlobals(globals: Environment) {
        globals.define("clock", object : LoxCallable {
            override fun call(interpreter: Interpreter, arguments: List<Any?>?): Any {
                return System.currentTimeMillis().toDouble() / 1000.0
            }

            override fun arity(): Int {
                return 0
            }

            override fun signature(): String {
                return "clock"
            }

            override fun toString(): String {
                return "<global fn 'clock'>"
            }
        })

        globals.define("exit", object : LoxCallable {
            override fun call(interpreter: Interpreter, arguments: List<Any?>?): Any? {
                exitProcess(((arguments?.get(0) ?: 1) as Double).toInt())
            }

            override fun arity(): Int {
                return 1
            }

            override fun signature(): String {
                return "exit"
            }

            override fun toString(): String {
                return "<global fn 'exit'>"
            }

        })

        globals.define("printerr", object : LoxCallable {
            override fun call(interpreter: Interpreter, arguments: List<Any?>?): Any? {
                System.err.println(arguments?.get(0) ?: "nil")
                return null
            }

            override fun arity(): Int {
                return 1
            }

            override fun signature(): String {
                return "printerr"
            }

            override fun toString(): String {
                return "<global fn 'printerr'>"
            }
        })

        globals.define("print", object : LoxCallable {
            override fun call(interpreter: Interpreter, arguments: List<Any?>?): Any? {
                println(arguments?.get(0) ?: "nil")
                return null
            }

            override fun arity(): Int {
                return 1
            }

            override fun signature(): String {
                return "print"
            }

            override fun toString(): String {
                return "<global fn 'print'>"
            }
        })

        globals.define("str", object : LoxCallable {
            override fun call(interpreter: Interpreter, arguments: List<Any?>?): Any {
                return arguments?.get(0).toString()
            }

            override fun arity(): Int {
                return 1
            }

            override fun signature(): String {
                return "str"
            }

            override fun toString(): String {
                return "<global fn 'str'>"
            }

        })

        globals.define("num", object : LoxCallable {
            override fun call(interpreter: Interpreter, arguments: List<Any?>?): Any {
                try {
                    return arguments?.get(0).toString().toDouble()
                } catch (e: NumberFormatException) {
                    return 0
                }
            }

            override fun arity(): Int {
                return 1
            }

            override fun signature(): String {
                return "num"
            }

            override fun toString(): String {
                return "<global fn 'num'>"
            }

        })

        globals.define("stacktrace", object : LoxCallable {
            override fun call(interpreter: Interpreter, arguments: List<Any?>?): Any? {
                var env: Environment? = interpreter.environment
                System.err.println("lox stacktrace:")
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

            override fun signature(): String {
                return "stacktrace"
            }

            override fun toString(): String {
                return "<global fn 'stacktrace'>"
            }

        })
    }
}