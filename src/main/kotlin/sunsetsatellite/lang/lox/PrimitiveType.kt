package sunsetsatellite.lang.lox

enum class PrimitiveType {
    ANY,
    NUMBER,
    STRING,
    BOOLEAN,
    FUNCTION,
    CLASS,
    OBJECT,
    NIL,
    UNKNOWN; //static type checker failed to get a more concrete type

    companion object {
        fun get(token: Token?): PrimitiveType {
            return when (token?.type) {
                TokenType.TYPE_ANY -> ANY
                TokenType.TYPE_STRING -> STRING
                TokenType.TYPE_BOOLEAN -> BOOLEAN
                TokenType.TYPE_FUNCTION -> FUNCTION
                TokenType.TYPE_NUMBER -> NUMBER
                TokenType.CLASS -> CLASS
                TokenType.IDENTIFIER -> OBJECT
                TokenType.NIL -> NIL
                else -> ANY
            }
        }
    }

    fun getName(): String {
        return name.lowercase()
    }
}