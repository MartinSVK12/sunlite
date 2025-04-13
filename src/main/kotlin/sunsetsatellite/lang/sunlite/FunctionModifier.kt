package sunsetsatellite.lang.sunlite

enum class FunctionModifier {
    NORMAL,
    INIT,
    STATIC,
    ABSTRACT,
    NATIVE;


    companion object {
        fun get(token: Token?): FunctionModifier {
            return when (token?.type) {
                TokenType.STATIC -> STATIC
                TokenType.NATIVE -> NATIVE
                else -> NORMAL
            }
        }
    }
}