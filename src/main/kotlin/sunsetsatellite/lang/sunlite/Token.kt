package sunsetsatellite.lang.sunlite

class Token(val type: TokenType, val lexeme: String, val literal: Any?, val line: Int, val file: String?, val pos: Position) {
	override fun toString(): String {
		return "$type $lexeme ${literal ?: ""}"
	}

	data class Position(val start: Int, val end: Int)

	companion object {
		fun unknown(): Token {
			return Token(TokenType.IDENTIFIER, "<unknown>", null, -1, null, Position(-1, -1))
		}

		fun identifier(name: String, line: Int = -1, file: String? = null ): Token {
			return Token(TokenType.IDENTIFIER, name, null, line, file, Position(-1, -1))
		}
	}

}