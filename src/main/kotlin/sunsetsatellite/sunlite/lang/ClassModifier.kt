package sunsetsatellite.sunlite.lang

enum class ClassModifier {
    NORMAL,
    ABSTRACT;

    companion object {
        fun get(token: Token?): ClassModifier {
            return when (token?.type) {
                TokenType.ABSTRACT -> ABSTRACT
                else -> NORMAL
            }
        }
    }
}