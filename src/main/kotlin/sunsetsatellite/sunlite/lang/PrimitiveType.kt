package sunsetsatellite.sunlite.lang

enum class PrimitiveType(val descriptor: Char, val isReference: Boolean = false) {
    ANY('A'),
    BYTE('b'),
    SHORT('s'),
    INT('i'),
    LONG('l'),
    FLOAT('f'),
    DOUBLE('d'),
    //NUMBER,
    STRING('t'),
    BOOLEAN('z'),
    FUNCTION('(', true),
    CLASS('C', true),
    OBJECT('O', true),
    ARRAY('[', true),
    TABLE('{', true),
    GENERIC('G'),
    NIL('N'),
    UNKNOWN('U'); //static type checker failed to get a more concrete type

    companion object {
        fun get(token: Token?): PrimitiveType {
            return when (token?.type) {
                TokenType.TYPE_ANY -> ANY
                TokenType.TYPE_STRING -> STRING
                TokenType.TYPE_BOOLEAN -> BOOLEAN
                TokenType.TYPE_FUNCTION -> FUNCTION
                TokenType.TYPE_BYTE -> BYTE
                TokenType.TYPE_SHORT -> SHORT
                TokenType.TYPE_INT -> INT
                TokenType.TYPE_LONG -> LONG
                TokenType.TYPE_FLOAT -> FLOAT
                TokenType.TYPE_DOUBLE -> DOUBLE
                //TokenType.TYPE_NUMBER -> NUMBER
                TokenType.TYPE_CLASS -> CLASS
                TokenType.IDENTIFIER -> OBJECT
                TokenType.TYPE_ARRAY -> ARRAY
                TokenType.TYPE_TABLE -> TABLE
                TokenType.TYPE_NIL -> NIL
                TokenType.QUESTION -> NIL
                TokenType.TYPE_GENERIC -> GENERIC
                else -> UNKNOWN
            }
        }
        
        fun get(descriptor: Char): PrimitiveType {
            return entries.find { it.descriptor == descriptor } ?: UNKNOWN
        }
    }

    fun getName(): String {
        return name.lowercase()
    }
}