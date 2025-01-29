package sunsetsatellite.lang.lox

enum class FunctionModifier {
    NONE,
    INIT,
    STATIC;


    companion object {
        fun get(token: Token?): FunctionModifier {
            return when (token?.type) {
                TokenType.STATIC -> STATIC
                else -> NONE
            }
        }
    }
}