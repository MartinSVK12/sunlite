package sunsetsatellite.lang.lox

interface LoxCallable {
	fun call(interpreter: Interpreter, arguments: List<Any?>?, typeArguments: List<Type>): Any?
	fun arity(): Int
	fun typeArity(): Int
	fun signature(): String
	fun varargs(): Boolean
}