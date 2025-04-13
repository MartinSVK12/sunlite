package sunsetsatellite.lang.sunlite

enum class PrimitiveType {
    ANY,
    NUMBER,
    STRING,
    BOOLEAN,
    FUNCTION,
    CLASS,
    OBJECT,
    ARRAY,
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
                TokenType.TYPE_ARRAY -> ARRAY
                TokenType.NIL -> NIL
                else -> UNKNOWN
            }
        }
    }

    fun getName(): String {
        return name.lowercase()
    }
}