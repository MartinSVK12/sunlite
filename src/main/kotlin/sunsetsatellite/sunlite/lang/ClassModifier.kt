package sunsetsatellite.sunlite.lang

enum class ClassModifier {
    NORMAL;

    companion object {
        fun get(token: Token?): ClassModifier {
            return when (token?.type) {
                //TokenType.DYNAMIC -> DYNAMIC
                else -> NORMAL
            }
        }
    }
}