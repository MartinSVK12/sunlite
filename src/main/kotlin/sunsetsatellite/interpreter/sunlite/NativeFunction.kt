package sunsetsatellite.interpreter.sunlite

fun interface NativeFunction<R> {
    fun call(interpreter: Interpreter, arguments: List<Any?>?, thisClass: LoxClassInstance?): R
}