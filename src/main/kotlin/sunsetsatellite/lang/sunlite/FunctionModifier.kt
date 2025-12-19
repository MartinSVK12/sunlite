package sunsetsatellite.lang.sunlite

enum class FunctionModifier {
    NORMAL,
    INIT,
    STATIC,
    ABSTRACT,
    NATIVE,
    STATIC_NATIVE,
    OPERATOR;


    companion object {
        fun get(token: Token?, token2: Token? = null): FunctionModifier {
            return if (token?.type == TokenType.STATIC && token2 != null && token2.type == TokenType.NATIVE) STATIC_NATIVE
            else if (token?.type == TokenType.STATIC) STATIC
            else if (token?.type == TokenType.NATIVE) NATIVE
            else if (token?.type == TokenType.OPERATOR) OPERATOR
            else NORMAL
        }
    }
}