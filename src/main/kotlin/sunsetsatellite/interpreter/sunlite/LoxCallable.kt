package sunsetsatellite.interpreter.sunlite

import sunsetsatellite.lang.sunlite.Type

interface LoxCallable {
	fun call(interpreter: Interpreter, arguments: List<Any?>?, typeArguments: List<Type>): Any?
	fun arity(): Int
	fun typeArity(): Int
	fun signature(): String
	fun varargs(): Boolean
}