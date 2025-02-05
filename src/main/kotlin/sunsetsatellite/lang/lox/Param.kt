package sunsetsatellite.lang.lox

data class Param(val token: Token, val type: Type) {
    override fun toString(): String {
        return "${token.lexeme}: ${type.getName()}"
    }
}