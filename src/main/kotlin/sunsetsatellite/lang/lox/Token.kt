package sunsetsatellite.lang.lox

class Token(val type: TokenType, val lexeme: String, val literal: Any?, val line: Int, val file: String?, val pos: Position) {
	override fun toString(): String {
		return "$type $lexeme ${literal ?: ""}"
	}

	data class Position(val start: Int, val end: Int)
}