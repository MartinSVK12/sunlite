package sunsetsatellite.sunlite.lang

enum class FunctionModifier(val s: String) {
    NORMAL(""),
    INIT(""),
    STATIC("static "),
    ABSTRACT("abstract "),
    NATIVE("native "),
    OPERATOR("operator "),
    OVERRIDE("override "),
    REQUIRED("required "),
    CHUNK("");


    companion object {
        fun get(token: Token?, token2: Token? = null): Array<FunctionModifier> {
            return if (token?.type == TokenType.STATIC && token2 != null && token2.type == TokenType.NATIVE) arrayOf(STATIC, NATIVE)
            else if (token?.type == TokenType.OVERRIDE && token2 != null && token2.type == TokenType.REQUIRED) arrayOf(OVERRIDE, REQUIRED)
            else if (token?.type == TokenType.OVERRIDE) arrayOf(OVERRIDE)
            else if (token?.type == TokenType.REQUIRED) arrayOf(REQUIRED)
            else if (token?.type == TokenType.STATIC) arrayOf(STATIC)
            else if (token?.type == TokenType.NATIVE) arrayOf(NATIVE)
            else if (token?.type == TokenType.OPERATOR) arrayOf(OPERATOR)
            else if (token?.type == TokenType.ABSTRACT) arrayOf(ABSTRACT)
            else arrayOf(NORMAL)
        }
    }

    override fun toString(): String {
        return s
    }
}