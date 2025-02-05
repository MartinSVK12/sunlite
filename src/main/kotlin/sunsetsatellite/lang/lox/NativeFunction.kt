package sunsetsatellite.lang.lox

fun interface NativeFunction<R> {
    fun call(interpreter: Interpreter, arguments: List<Any?>?, thisClass: LoxClassInstance?): R
}