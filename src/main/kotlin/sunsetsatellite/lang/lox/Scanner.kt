package sunsetsatellite.lang.lox

import sunsetsatellite.lang.lox.Lox.Companion.stacktrace
import sunsetsatellite.lang.lox.TokenType.*


class Scanner(private val source: String, val lox: Lox) {
	private val tokens: ArrayList<Token> = ArrayList()
	private var start = 0
	private var current = 0
	private var line = 1
	private var totalChars = 0
	private var lineStart = 0
	private var lineCurrent = 0

	var currentFile: String? = null

	companion object {
		private val keywords: MutableMap<String, TokenType> = mutableMapOf()

		init {
			keywords["and"] = AND
			keywords["class"] = CLASS
			keywords["else"] = ELSE
			keywords["false"] = FALSE
			keywords["for"] = FOR
			keywords["fun"] = FUN
			keywords["if"] = IF
			keywords["nil"] = NIL
			keywords["or"] = OR
			//keywords["print"] = PRINT
			keywords["return"] = RETURN
			keywords["super"] = SUPER
			keywords["this"] = THIS
			keywords["true"] = TRUE
			keywords["var"] = VAR
			keywords["while"] = WHILE
			keywords["break"] = BREAK
			keywords["continue"] = CONTINUE
			keywords["static"] = STATIC
			keywords["native"] = NATIVE
			keywords["interface"] = INTERFACE
			keywords["is"] = IS
			keywords["isnt"] = IS_NOT
			keywords["import"] = IMPORT
			keywords["dynamic"] = DYNAMIC
			keywords["any"] = TYPE_ANY
			keywords["string"] = TYPE_STRING
			keywords["number"] = TYPE_NUMBER
			keywords["boolean"] = TYPE_BOOLEAN
			keywords["function"] = TYPE_FUNCTION
			keywords["array"] = TYPE_ARRAY
			keywords["as"] = AS
			keywords["extends"] = EXTENDS
			keywords["implements"] = IMPLEMENTS
		}
	}


	fun scanTokens(path: String?): List<Token> {
		currentFile = path

		while (!isAtEnd()) {
			// We are at the beginning of the next lexeme.
			start = current
			lineStart = lineCurrent
			scanToken()
		}

		tokens.add(Token(EOF, "", null, line, currentFile, Token.Position(-1,-1)))
		return tokens
	}

	private fun isAtEnd(): Boolean {
		return current >= source.length
	}

	private fun scanToken() {
		when (val c = advance()) {
			'(' -> addToken(LEFT_PAREN)
			')' -> addToken(RIGHT_PAREN)
			'{' -> addToken(LEFT_BRACE)
			'}' -> addToken(RIGHT_BRACE)
			'[' -> addToken(LEFT_BRACKET)
			']' -> addToken(RIGHT_BRACKET)
			',' -> addToken(COMMA)
			'.' -> addToken(DOT)
			'-' -> addToken(if(match('=')) MINUS_EQUAL else MINUS)
			'+' -> addToken(if(match('=')) PLUS_EQUAL else PLUS)
			';' -> addToken(SEMICOLON)
			'*' -> addToken(STAR)
			':' -> addToken(COLON)
			'|' -> addToken(PIPE)
			'!' -> addToken(if(match('=')) BANG_EQUAL else BANG)
			'=' -> addToken(if(match('=')) EQUAL_EQUAL else EQUAL)
			'<' -> addToken(if(match('=')) LESS_EQUAL else LESS)
			'>' -> addToken(if(match('=')) GREATER_EQUAL else GREATER)
			'/' -> {
				if(match('/')){
					while (peek() != '\n' && !isAtEnd()) advance()
				} else if(match('*')){
					while (peek() != '*' && peekNext() != '/' && !isAtEnd()) advance()
					if(!isAtEnd() && peek() == '*' && peekNext() == '/') advance(); advance()
				}
				else {
					addToken(SLASH)
				}
			}
			' ', '\r', '\t' -> {}
			'\n' -> {
				lineStart = 0
				lineCurrent = 0
				line++
			}
			'"' -> string()
			in '0'..'9' -> number()

			else -> {

				if(isAlpha(c)) {
					identifier()
					return
				}

				lox.error(line, "Unexpected character '$c'")
			}
		}
	}

	private fun match(expected: Char): Boolean {
		if (isAtEnd()) return false
		if (source[current] != expected) return false

		current++
		totalChars++
		lineCurrent++
		return true
	}

	private fun peek(): Char {
		if (isAtEnd()) return '\u0000'
		return source[current]
	}

	private fun peekNext(): Char {
		if (current + 1 >= source.length) return '\u0000'
		return source[current + 1]
	}


	private fun advance(): Char {
		totalChars++
		lineCurrent++
		return source[current++]
	}

	private fun addToken(type: TokenType) {
		addToken(type, null)
	}

	private fun addToken(type: TokenType, literal: Any?) {
		val text = source.substring(start, current)
		tokens.add(Token(type, text, literal, line, currentFile, Token.Position(lineStart,lineCurrent)))
	}

	private fun string() {
		while (peek() != '"' && !isAtEnd()) {
			if (peek() == '\n') line++
			advance()
		}

		if (isAtEnd()) {
			lox.error(line, "Unterminated string.")
			return
		}

		// The closing ".
		advance()

		// Trim the surrounding quotes.
		val value = source.substring(start + 1, current - 1)
		addToken(STRING, value)
	}

	private fun number() {
		while (peek() in '0'..'9') advance()

		// Look for a fractional part.
		if (peek() == '.' && peekNext() in '0'..'9') {
			// Consume the "."
			advance()

			while (peek() in '0'..'9') advance()
		}

		addToken(
			NUMBER,
			source.substring(start, current).toDouble()
		)
	}

	private fun identifier() {
		while (isAlphaNumeric(peek())) advance()


		val text = source.substring(start, current)
		var type = keywords[text]
		if (type == null) type = IDENTIFIER
		addToken(type)

	}

	private fun isAlpha(c: Char): Boolean {
		return (c in 'a'..'z') ||
				(c in 'A'..'Z') || c == '_'
	}

	private fun isAlphaNumeric(c: Char): Boolean {
		return isAlpha(c) || c in '0'..'9'
	}


}