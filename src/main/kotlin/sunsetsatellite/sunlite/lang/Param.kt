package sunsetsatellite.sunlite.lang

data class Param(val token: Token, val type: Type) {

    constructor(type: Type): this(Token.identifier(""),type)

    override fun toString(): String {
        return "${token.lexeme}: $type"
    }
}