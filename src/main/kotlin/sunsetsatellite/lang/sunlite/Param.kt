package sunsetsatellite.lang.sunlite

data class Param(val token: Token, val type: Type) {
    override fun toString(): String {
        return "${token.lexeme}: ${type.getName()}"
    }
}