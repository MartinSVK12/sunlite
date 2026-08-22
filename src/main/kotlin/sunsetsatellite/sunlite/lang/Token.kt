package sunsetsatellite.sunlite.lang

class Token(
    val type: TokenType,
    val lexeme: String,
    val literal: Any?,
    val line: Int,
    val file: String?,
    val pos: Position
) {

	constructor(map: Map<String, Any?>) : this(
		type = TokenType.valueOf(map["name"].toString()),
		lexeme = map["lexeme"].toString(),
		literal = null,
		file = map["file"].toString(),
		line = (map["line"] as Long).toInt(),
		pos = Position(((map["pos"] as Map<*, *>)["x"] as Long).toInt(), ((map["pos"] as Map<*, *>)["y"] as Long).toInt())
	)

    data class Position(val start: Int, val end: Int) {
        override fun toString(): String {
            return "$start:$end"
        }
    }

    companion object {
        fun unknown(): Token {
            return Token(TokenType.IDENTIFIER, "<unknown>", null, -1, null, Position(-1, -1))
        }

        fun identifier(name: String, line: Int = -1, file: String? = null): Token {
            return Token(TokenType.IDENTIFIER, name, null, line, file, Position(-1, -1))
        }

        fun identifier(name: String, expr: Expr): Token {
            return Token(TokenType.IDENTIFIER, name, null, expr.getLine(), expr.getFile(), Position(-1, -1))
        }

        fun identifier(name: String, token: Token): Token {
            return Token(TokenType.IDENTIFIER, name, null, token.line, token.file, Position(-1, -1))
        }
    }

    override fun toString(): String {
        return "$type $lexeme ${literal ?: ""}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Token) return false

        if (line != other.line) return false
        if (type != other.type) return false
        if (lexeme != other.lexeme) return false
        if (literal != other.literal) return false
        if (file != other.file) return false
        if (pos != other.pos) return false

        return true
    }

    override fun hashCode(): Int {
        var result = line
        result = 31 * result + type.hashCode()
        result = 31 * result + lexeme.hashCode()
        result = 31 * result + (literal?.hashCode() ?: 0)
        result = 31 * result + (file?.hashCode() ?: 0)
        result = 31 * result + pos.hashCode()
        return result
    }

}