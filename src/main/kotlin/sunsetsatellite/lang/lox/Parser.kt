package sunsetsatellite.lang.lox

import sunsetsatellite.lang.lox.Expr.*
import sunsetsatellite.lang.lox.TokenType.*


class Parser(val tokens: List<Token>) {
	private var current = 0

	private class ParseError : RuntimeException()

	fun parse(): List<Stmt> {
		val statements: MutableList<Stmt> = ArrayList()

		while (!isAtEnd()) {
			declaration()?.let { statements.add(it) }
		}

		return statements
	}

	private fun declaration(): Stmt? {
		try {
			if (match(VAR)) return varDeclaration()

			return statement()
		} catch (error: ParseError) {
			synchronize()
			return null
		}
	}

	private fun statement(): Stmt {
		when {
			match(PRINT) -> return printStatement()
			match(LEFT_BRACE) -> return Stmt.Block(block())
			match(IF) -> return ifStatement()
			match(WHILE) -> return whileStatement()
			match(FOR) -> return forStatement()
			match(BREAK) -> return breakStatement()
			match(CONTINUE) -> return continueStatement()
			match(FUN) -> return function("function", null)
			match(RETURN) -> return returnStatement()
			match(CLASS) -> return classDeclaration()
			else -> return expressionStatement()
		}
	}

	private fun classDeclaration(): Stmt {
		val name = consume(IDENTIFIER, "Expected class name.")


		var superclass: Variable? = null
		if (match(LESS)) {
			consume(IDENTIFIER, "Expected superclass name.")
			superclass = Variable(previous())
		}

		consume(LEFT_BRACE, "Expected '{' before class body.")

		val methods: MutableList<Stmt.Function> = ArrayList()
		while (!check(RIGHT_BRACE) && !isAtEnd()) {
			if(match(STATIC)) {
				methods.add(function("method", previous()))
			} else {
				methods.add(function("method", null))
			}
		}

		consume(RIGHT_BRACE, "Expected '}' after class body.")

		return Stmt.Class(name, methods, superclass)
	}

	private fun function(kind: String, modifier: Token?): Stmt.Function {
		if(modifier != null && FunctionModifier.entries.none { it.name.lowercase() == modifier.type.name.lowercase() }) {
			error(peek(), "Invalid $kind modifier '${modifier.lexeme}'.")
		}
		val name = consume(IDENTIFIER, "Expected $kind name.")
		consume(LEFT_PAREN, "Expect '(' after $kind name.")
		val parameters: MutableList<Token> = ArrayList()
		if (!check(RIGHT_PAREN)) {
			do {
				if (parameters.size >= 255) {
					error(peek(), "Can't have more than 255 parameters.")
				}

				parameters.add(
					consume(IDENTIFIER, "Expected parameter name.")
				)
			} while (match(COMMA))
		}
		consume(RIGHT_PAREN, "Expected ')' after parameters.")

		consume(LEFT_BRACE, "Expected '{' before $kind body.")
		val body = block()
		return Stmt.Function(name, parameters, body, FunctionModifier.get(modifier))
	}

	private fun returnStatement(): Stmt {
		val keyword = previous()
		var value: Expr? = null
		if (!check(SEMICOLON)) {
			value = expression()
		}

		consume(SEMICOLON, "Expected ';' after return value.")
		return Stmt.Return(keyword, value)
	}

	private fun breakStatement(): Stmt {
		consume(SEMICOLON, "Expected ';' after 'break'.")
		val keyword = previous()
		//if(!inLoop) throw error(peek(),"Unexpected 'break' outside of loop.")
		return Stmt.Break(keyword)
	}

	private fun continueStatement(): Stmt {
		consume(SEMICOLON, "Expected ';' after 'continue'.")
		val keyword = previous()
		//if(!inLoop) throw error(peek(),"Unexpected 'continue' outside of loop.")
		return Stmt.Continue(keyword)
	}

	private fun printStatement(): Stmt {
		val value = expression()
		consume(SEMICOLON, "Expected ';' after value.")
		return Stmt.Print(value)
	}

	private fun whileStatement(): Stmt {
		consume(LEFT_PAREN, "Expected '(' after 'while'.")
		val condition = expression()
		consume(RIGHT_PAREN, "Expected ')' after 'while' condition.")
		//inLoop = true
		val body = statement()
		//inLoop = false

		return Stmt.While(condition, body)
	}

	private fun forStatement(): Stmt {
		consume(LEFT_PAREN, "Expected '(' after 'for'.")
		val initializer = if (match(SEMICOLON)) {
			null
		} else if (match(VAR)) {
			varDeclaration()
		} else {
			expressionStatement()
		}

		var condition: Expr? = null
		if (!check(SEMICOLON)) {
			condition = expression()
		}
		consume(SEMICOLON, "Expected ';' after loop condition.")

		var increment: Expr? = null
		if (!check(RIGHT_PAREN)) {
			increment = expression()
		}
		consume(RIGHT_PAREN, "Expected ')' after 'for' clauses.")

		//inLoop = true

		var body = statement()

		if (increment != null) {
			body = Stmt.Block(
				listOf(
					body,
					Stmt.Expression(increment)
				)
			)
		}

		if (condition == null) condition = Literal(true)
		body = Stmt.While(condition, body)

		if (initializer != null) {
			body = Stmt.Block(listOf(initializer, body))
		}

		//inLoop = false

		return body
	}

	private fun ifStatement(): Stmt {
		consume(LEFT_PAREN, "Expected '(' after 'if'.")
		val condition = expression()
		consume(RIGHT_PAREN, "Expected ')' after 'if' condition.")

		val thenBranch = statement()
		var elseBranch: Stmt? = null
		if (match(ELSE)) {
			elseBranch = statement()
		}

		return Stmt.If(condition, thenBranch, elseBranch)
	}

	private fun varDeclaration(): Stmt {
		val name = consume(IDENTIFIER, "Expected variable name.")

		var initializer: Expr? = null
		if (match(EQUAL)) {
			initializer = expression()
		}

		consume(SEMICOLON, "Expected ';' after variable declaration.")
		return Stmt.Var(name, initializer)
	}

	private fun expressionStatement(): Stmt {
		val expr = expression()
		consume(SEMICOLON, "Expected ';' after expression.")
		return Stmt.Expression(expr)
	}

	private fun block(): List<Stmt> {
		val statements: MutableList<Stmt> = ArrayList()

		while (!check(RIGHT_BRACE) && !isAtEnd()) {
			declaration()?.let { statements.add(it) }
		}

		consume(RIGHT_BRACE, "Expected '}' after block.")
		return statements
	}


	private fun expression(): Expr {
		return assignment()
	}

	private fun assignment(): Expr {
		val expr = orExpr()

		if (match(EQUAL)) {
			val equals = previous()
			val value = assignment()

			if (expr is Variable) {
				val name = expr.name
				return Assign(name, value)
			} else if (expr is Get) {
				return Set(expr.obj, expr.name, value)
			}

			error(equals, "Invalid assignment target.")
		}

		return expr
	}

	private fun orExpr(): Expr {
		var expr: Expr = andExpr()

		while (match(OR)) {
			val operator = previous()
			val right: Expr = andExpr()
			expr = Logical(expr, operator, right)
		}

		return expr
	}

	private fun andExpr(): Expr {
		var expr = equality()

		while (match(AND)) {
			val operator = previous()
			val right = equality()
			expr = Logical(expr, operator, right)
		}

		return expr
	}

	private fun equality(): Expr {
		var expr: Expr = comparison()

		while (match(BANG_EQUAL, EQUAL_EQUAL)) {
			val operator: Token = previous()
			val right: Expr = comparison()
			expr = Binary(expr, operator, right)
		}

		return expr
	}

	private fun comparison(): Expr {
		var expr: Expr = term()

		while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
			val operator = previous()
			val right: Expr = term()
			expr = Binary(expr, operator, right)
		}

		return expr
	}

	private fun term(): Expr {
		var expr: Expr = factor()

		while (match(MINUS, PLUS)) {
			val operator = previous()
			val right: Expr = factor()
			expr = Binary(expr, operator, right)
		}

		return expr
	}

	private fun factor(): Expr {
		var expr: Expr = unary()

		while (match(SLASH, STAR)) {
			val operator = previous()
			val right: Expr = unary()
			expr = Binary(expr, operator, right)
		}

		return expr
	}

	private fun unary(): Expr {
		if (match(BANG, MINUS)) {
			val operator = previous()
			val right = unary()
			return Unary(operator, right)
		}

		return lambda()
	}

	private fun lambda(): Expr {
		if(match(FUN)){
			while (true) {
				if (match(LEFT_PAREN)) {
					return finishLambda()
				} else {
					break
				}
			}
		}
		return call()
	}

	private fun call(): Expr {
		var expr = primary()

		while (true) {
			if (match(LEFT_PAREN)) {
				expr = finishCall(expr)
			} else if (match(DOT)) {
				val name: Token = consume(IDENTIFIER, "Expected property name after '.'.")
				expr = Get(expr, name)
			} else {
				break
			}
		}

		return expr
	}

	private fun finishLambda(): Expr {
		val parameters: MutableList<Token> = ArrayList()
		if (!check(RIGHT_PAREN)) {
			do {
				if (parameters.size >= 255) {
					error(peek(), "Can't have more than 255 parameters.")
				}

				parameters.add(
					consume(IDENTIFIER, "Expected parameter name.")
				)
			} while (match(COMMA))
		}
		consume(RIGHT_PAREN, "Expected ')' after parameters.")

		consume(LEFT_BRACE, "Expected '{' before lambda body.")
		val body = block()

		return Lambda(Stmt.Function(Token(STRING,"<lambda>", null, peek().line) ,parameters, body, FunctionModifier.NONE))
	}

	private fun finishCall(callee: Expr): Expr {
		val arguments: MutableList<Expr> = ArrayList()
		if (!check(RIGHT_PAREN)) {
			do {
				if (arguments.size >= 255) {
					error(peek(), "Can't have more than 255 arguments.")
				}
				arguments.add(expression())
			} while (match(COMMA))
		}

		val paren = consume(
			RIGHT_PAREN,
			"Expected ')' after arguments."
		)

		return Call(callee, paren, arguments)
	}

	private fun primary(): Expr {
		if (match(FALSE)) return Literal(false)
		if (match(TRUE)) return Literal(true)
		if (match(NIL)) return Literal(null)

		if (match(NUMBER, STRING)) {
			return Literal(previous().literal)
		}

		if (match(SUPER)) {
			val keyword = previous()
			consume(DOT, "Expected '.' after 'super'.")
			val method = consume(
				IDENTIFIER,
				"Expected superclass method name."
			)
			return Super(keyword, method)
		}

		if (match(THIS)) return This(previous())

		if (match(LEFT_PAREN)) {
			val expr = expression()
			consume(RIGHT_PAREN, "Expected ')' after expression.")
			return Grouping(expr)
		}

		if (match(IDENTIFIER)) {
			return Variable(previous())
		}

		throw error(peek(), "Expected expression.")
	}

	private fun consume(type: TokenType, message: String): Token {
		if (check(type)) return advance()

		throw error(peek(), message)
	}

	private fun match(vararg types: TokenType): Boolean {
		for (type in types) {
			if (check(type)) {
				advance()
				return true
			}
		}

		return false
	}

	private fun check(type: TokenType): Boolean {
		if (isAtEnd()) return false
		return peek().type === type
	}

	private fun advance(): Token {
		if (!isAtEnd()) current++
		return previous()
	}

	private fun isAtEnd(): Boolean {
		return peek().type == EOF
	}

	private fun peek(): Token {
		return tokens[current]
	}

	private fun previous(): Token {
		return tokens[current - 1]
	}

	private fun error(token: Token, message: String): ParseError {
		Lox.error(token, message)
		return ParseError()
	}

	private fun synchronize() {
		advance()

		while (!isAtEnd()) {
			if (previous().type == SEMICOLON) return

			when (peek().type) {
				CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN, BREAK -> return
				else -> {}
			}

			advance()
		}
	}


}