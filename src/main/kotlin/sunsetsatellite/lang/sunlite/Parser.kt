package sunsetsatellite.lang.sunlite

import sunsetsatellite.lang.sunlite.Expr.*
import sunsetsatellite.lang.sunlite.TokenType.*
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


class Parser(val tokens: List<Token>, val sunlite: Sunlite) {
	private var current = 0

	var currentFile: String? = null
	var currentClass: Token? = null
	var currentFunction: Token? = null

	private class ParseError : RuntimeException()

	fun parse(path: String?): List<Stmt> {
		currentFile = path
		val statements: MutableList<Stmt> = ArrayList()

		while (!isAtEnd()) {
			declaration()?.let { statements.add(it) }
		}

		return statements
	}

	private fun declaration(): Stmt? {
		try {
			return when {
				match(VAR) -> varDeclaration()
				match(FUN) -> function(FunctionType.FUNCTION, null)
				//match(DYNAMIC) -> classDeclaration(ClassModifier.DYNAMIC)
				match(CLASS) -> classDeclaration(ClassModifier.NORMAL)
				match(INTERFACE) -> interfaceDeclaration()
				match(IMPORT) -> importStatement()
				else -> statement()
			}
		} catch (error: ParseError) {
			synchronize()
			return null
		}
	}

	private fun statement(): Stmt {
		return when {
			match(THROW) -> throwStatement()
			//match(PRINT) -> printStatement()
			match(LEFT_BRACE) -> Stmt.Block(block(), previous().line, previous().file)
			match(IF) -> ifStatement()
			match(WHILE) -> whileStatement()
			match(FOR) -> forStatement()
			match(BREAK) -> breakStatement()
			match(CONTINUE) -> continueStatement()
			match(RETURN) -> returnStatement()
			match(TRY) -> tryCatchStatement()
			else -> expressionStatement()
		}
	}

	private fun tryCatchStatement(): Stmt {
		val tryToken = previous()
		consume(LEFT_BRACE, "Expected '{' before try block.")
		val tryBlock = block()
		consume(CATCH, "Expected 'catch' after try block.")
		val catchToken = previous()
		consume(LEFT_PAREN, "Expected '(' after 'catch'.")
		val catchVariable = Param(consume(IDENTIFIER, "Expected catch variable name."), getType())
		consume(RIGHT_PAREN, "Expected ')' after catch variable.")
		consume(LEFT_BRACE, "Expected '{' before catch block.")
		val catchBlock = block()
		return Stmt.TryCatch(
			tryToken,
			catchToken,
			Stmt.Block(tryBlock, previous().line, previous().file),
			catchVariable,
			Stmt.Block(catchBlock, previous().line, previous().file))
	}

	private fun importStatement(): Stmt? {
		val keyword = previous()
		val what = consume(STRING, "Expected import location string.")
		consume(SEMICOLON, "Expected ';' after import statement.")

		if(sunlite.imports.contains(what.literal as String)){
			return null
		}

		var bytes: ByteArray? = null
		var actualPath: Path
		val invalidPaths: MutableList<Path> = mutableListOf()

		sunlite.path.forEach {
			actualPath = Paths.get(it, what.literal)
			try {
				bytes = Files.readAllBytes(actualPath)
			} catch (_: IOException) {
				invalidPaths.add(actualPath)
			}
		}

		if(bytes == null) {
			sunlite.error(keyword, "Load error, couldn't find '${what.literal}' on the load path list.")
		}

		val scanner = Scanner(String(bytes!!, Charset.defaultCharset()),sunlite)
		val tokens: List<Token> = scanner.scanTokens(what.literal)

		if(Sunlite.debug){
			println("Tokens: ")
			println("--------")
			val builder: StringBuilder = StringBuilder()
			tokens.forEachIndexed { i, it ->
				builder.append("($it)")
				if(tokens.size-1 > i) builder.append(", ")
				if(i != 0 && i % 10 == 0) builder.append("\n")
			}
			println(builder.toString())
			println()
		}

		val parser = Parser(tokens,sunlite)
		val statements = parser.parse(what.literal)

		// Stop if there was a syntax error.
		if (sunlite.hadError) sunlite.error(keyword, "Load error at '${what.literal}' (details above).")

		sunlite.imports[what.literal] = statements

		if(Sunlite.debug) {
			println("AST: ")
			println("-----")
			statements.forEach {
				println(AstPrinter.print(it))
			}
			println("-----")
			println()
		}

		if(Sunlite.debug) {
			println("parsed '${what.literal}'")
			println("------------------------")
			println()
		}

		return Stmt.Import(keyword, what)
	}

	private fun classDeclaration(modifier: ClassModifier): Stmt {
		//if(modifier == ClassModifier.DYNAMIC) consume(CLASS, "Expected 'class' after class modifier.")

		val typeParameters: MutableList<Param> = ArrayList()
		if(match(LESS)){
			do {
				if (typeParameters.size >= 255) {
					error(peek(), "Can't have more than 255 type parameters.")
				}

				val identifier = consume(IDENTIFIER, "Expected type parameter name.")
				typeParameters.add(Param(identifier, Type.Parameter(identifier)))
			} while (match(COMMA))
			consume(GREATER, "Expected '>' after type parameter declaration.")
		}

		val name = consume(IDENTIFIER, "Expected class name.")

		currentClass = name

		var superclass: Variable? = null
		if (match(EXTENDS)) {
			consume(IDENTIFIER, "Expected superclass name.")
			superclass = Variable(previous())
		}

		val superinterfaces: MutableList<Variable> = mutableListOf()
		if (match(IMPLEMENTS)) {
			do {
				if (superinterfaces.size >= 255) {
					error(peek(), "Can't inherit more than 255 superinterfaces.")
				}

				superinterfaces.add(
					Variable(consume(IDENTIFIER, "Expected superinterface name."))
				)
			} while (match(COMMA))
		}

		consume(LEFT_BRACE, "Expected '{' before class body.")

		val methods: MutableList<Stmt.Function> = ArrayList()
		val fields: MutableList<Stmt.Var> = ArrayList()
		while (!checkType(RIGHT_BRACE) && !isAtEnd()) {
			val currentModifier = peek()
			when {
				match(STATIC) || match(NATIVE) -> {
					if(previous().type == STATIC) {
						when {
							match(VAR) -> {
								fields.add(varDeclaration(FieldModifier.STATIC))
							}
							match(FUN) -> {
								methods.add(function(FunctionType.METHOD, currentModifier))
							}
							else -> {
								throw error(peek(), "Expected a field or method declaration.")
							}
						}
					} else if(match(FUN)) {
						methods.add(function(FunctionType.METHOD, currentModifier))
					} else {
						throw error(peek(), "Expected a field or method declaration.")
					}
				}
				match(VAR) -> {
					fields.add(varDeclaration())
				}
				match(FUN) -> {
					methods.add(function(FunctionType.METHOD, null))
				}
				match(INIT) -> {
					methods.add(function(FunctionType.INITIALIZER, null))
				}
				else -> {
					throw error(peek(), "Expected a field or method declaration.")
				}
			}
		}

		consume(RIGHT_BRACE, "Expected '}' after class body.")

		currentClass = null

		return Stmt.Class(name, methods, fields, superclass, superinterfaces, modifier, typeParameters)
	}

	private fun interfaceDeclaration(): Stmt {

		val typeParameters: MutableList<Param> = ArrayList()
		if(match(LESS)){
			do {
				if (typeParameters.size >= 255) {
					error(peek(), "Can't have more than 255 type parameters.")
				}

				val identifier = consume(IDENTIFIER, "Expected type parameter name.")
				typeParameters.add(Param(identifier, Type.Parameter(identifier)))
			} while (match(COMMA))
			consume(GREATER, "Expected '>' after type parameter declaration.")
		}

		val name = consume(IDENTIFIER, "Expected class name.")

		val superinterfaces: MutableList<Variable> = mutableListOf()
		if (match(IMPLEMENTS)) {
			do {
				if (superinterfaces.size >= 255) {
					error(peek(), "Can't inherit more than 255 superinterfaces.")
				}

				superinterfaces.add(
					Variable(consume(IDENTIFIER, "Expected superinterface name."))
				)
			} while (match(COMMA))
		}

		consume(LEFT_BRACE, "Expected '{' before class body.")

		val methods: MutableList<Stmt.Function> = ArrayList()
		while (!checkType(RIGHT_BRACE) && !isAtEnd()) {
			methods.add(abstractMethod())
		}

		consume(RIGHT_BRACE, "Expected '}' after class body.")

		return Stmt.Interface(name, methods, superinterfaces, typeParameters)
	}

	private fun funcSignature(kind: FunctionType): Triple<Token,List<Param>, Type>{
		val name = if(kind == FunctionType.INITIALIZER) Token.identifier("init") else consume(IDENTIFIER, "Expected ${kind.toString().lowercase()} name.")
		currentFunction = name
		consume(LEFT_PAREN, "Expected '(' after ${kind.toString().lowercase()} name.")
		val parameters: MutableList<Param> = ArrayList()
		if (!checkType(RIGHT_PAREN)) {
			do {
				if (parameters.size >= 255) {
					error(peek(), "Can't have more than 255 parameters.")
				}

				parameters.add(
					Param(consume(IDENTIFIER, "Expected parameter name."), getType())
				)
			} while (match(COMMA))
		}
		consume(RIGHT_PAREN, "Expected ')' after parameters.")

		val type = getType(function = true)

		return Triple(name,parameters,type)
	}

	private fun abstractMethod(): Stmt.Function {
		consume(FUN, "Expected abstract method declaration")

		val typeParameters: MutableList<Param> = ArrayList()
		if(match(LESS)){
			do {
				if (typeParameters.size >= 255) {
					error(peek(), "Can't have more than 255 type parameters.")
				}

				val identifier = consume(IDENTIFIER, "Expected type parameter name.")
				typeParameters.add(Param(identifier, Type.Parameter(identifier)))
			} while (match(COMMA))
			consume(GREATER, "Expected '>' after type parameter declaration.")
		}

		val signature = funcSignature(FunctionType.METHOD)
		currentFunction = null

		return Stmt.Function(
			signature.first,
			FunctionType.METHOD,
			signature.second,
			listOf(),
			FunctionModifier.ABSTRACT,
			signature.third,
			typeParameters
		)
	}

	private fun function(kind: FunctionType, modifier: Token?): Stmt.Function {
		if(modifier != null && FunctionModifier.entries.none { it.name.lowercase() == modifier.type.name.lowercase() }) {
			error(peek(), "Invalid ${kind.toString().lowercase()} modifier '${modifier.lexeme}'.")
		}

		val typeParameters: MutableList<Param> = ArrayList()
		if(match(LESS)){
			do {
				if (typeParameters.size >= 255) {
					error(peek(), "Can't have more than 255 type parameters.")
				}

				val identifier = consume(IDENTIFIER, "Expected type parameter name.")
				typeParameters.add(Param(identifier, Type.Parameter(identifier)))
			} while (match(COMMA))
			consume(GREATER, "Expected '>' after type parameter declaration.")
		}

		val funcModifier = FunctionModifier.get(modifier)
		val signature = funcSignature(kind)

		var body: List<Stmt> = listOf()
		if(funcModifier != FunctionModifier.NATIVE){
			consume(LEFT_BRACE, "Expected '{' before ${kind.toString().lowercase()} body.")
			body = block()
		} else {
			assert(LEFT_BRACE, "Native ${kind.toString().lowercase()} cannot have a body.")
		}

		currentFunction = null

		return Stmt.Function(signature.first, kind, signature.second, body, funcModifier, signature.third, typeParameters)
	}

	private fun returnStatement(): Stmt {
		val keyword = previous()
		var value: Expr? = null
		if (!checkType(SEMICOLON)) {
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
		throw error(peek(),"Print as a statement has been deprecated, please use the global function 'print(...)' instead.")
		/*val value = expression()
		consume(SEMICOLON, "Expected ';' after value.")
		return Stmt.Print(value)*/
	}

	private fun throwStatement(): Stmt {
		val value = expression()
		consume(SEMICOLON, "Expected ';' after value.")
		return Stmt.Throw(value)
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
		if (!checkType(SEMICOLON)) {
			condition = expression()
		}
		consume(SEMICOLON, "Expected ';' after loop condition.")

		var increment: Expr? = null
		if (!checkType(RIGHT_PAREN)) {
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
				), peek().line, peek().file
			)
		}

		if (condition == null) condition = Literal(true,peek().line,peek().file)
		body = Stmt.While(condition, body)

		if (initializer != null) {
			body = Stmt.Block(listOf(initializer, body), peek().line, peek().file)
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

	private fun varDeclaration(modifier: FieldModifier = FieldModifier.NORMAL): Stmt.Var {
		val name = consume(IDENTIFIER, "Expected variable name.")

		val type = getType()

		var initializer: Expr? = null
		if (match(EQUAL)) {
			initializer = expression()
		}

		consume(SEMICOLON, "Expected ';' after variable declaration.")
		return Stmt.Var(name, type, initializer, modifier)
	}

	private fun getTypeTokens(oneOnly: Boolean): List<TypeToken> {
		var firstToken = peek()
		var firstTypeParameter: List<TypeToken> = listOf()
		var firstPureTypeParameter = false
		if (!match(TYPE_BOOLEAN, TYPE_STRING, TYPE_NUMBER, TYPE_FUNCTION, CLASS, TYPE_ANY, TYPE_ARRAY, IDENTIFIER, NIL)) {
			if(match(LESS)){
				if(oneOnly){
					throw error(firstToken, "Only bounds can be specified for pure type parameter.")
				}
				firstToken = peek()
				firstTypeParameter = getTypeTokens(true)
				consume(GREATER, "Expected '>' after type parameter.")
				firstPureTypeParameter = true
			} else {
				throw error(firstToken, "Expected type.")
			}
		}
		if(!firstPureTypeParameter && match(LESS)) {
			if(oneOnly){
				throw error(firstToken, "Only bounds can be specified for pure type parameter.")
			}
			if (!checkType(TYPE_BOOLEAN, TYPE_STRING, TYPE_NUMBER, TYPE_FUNCTION, CLASS, TYPE_ANY, TYPE_ARRAY, IDENTIFIER, NIL, LESS)) {
				throw error(firstToken, "Expected type for type parameter.")
			}
			firstTypeParameter = getTypeTokens(false)
			consume(GREATER, "Expected '>' after type parameter.")
		}
		val types: MutableList<TypeToken> = mutableListOf(TypeToken(firstToken, if(firstPureTypeParameter) listOf() else firstTypeParameter, firstPureTypeParameter))

		do {
			if (types.size >= 255) {
				error(peek(), "Can't have more than 255 types in a union type.")
			}

			if(match(PIPE)) {
				var additionalToken = peek()
				var additionalPureTypeParameter = false
				var additionalTypeParameter: List<TypeToken> = listOf()
				if (!match(
						TYPE_BOOLEAN,
						TYPE_STRING,
						TYPE_NUMBER,
						TYPE_FUNCTION,
						CLASS,
						TYPE_ANY,
						TYPE_ARRAY,
						IDENTIFIER,
						NIL,
					)
				) {
					if(match(LESS)){
						if(oneOnly){
							throw error(firstToken, "Only bounds can be specified for pure type parameter.")
						}
						additionalToken = peek()
						additionalTypeParameter = getTypeTokens(true)
						consume(GREATER, "Expected '>' after type parameter.")
						additionalPureTypeParameter = true
					} else {
						throw error(additionalToken, "Expected type.")
					}
				} else {
					if(match(LESS)){
						if(oneOnly){
							throw error(firstToken, "Only bounds can be specified for pure type parameter.")
						}
						if (!checkType(TYPE_BOOLEAN, TYPE_STRING, TYPE_NUMBER, TYPE_FUNCTION, CLASS, TYPE_ANY, TYPE_ARRAY, IDENTIFIER, NIL, LESS)) {
							throw error(firstToken, "Expected type for type parameter.")
						}
						additionalTypeParameter = getTypeTokens(false)
						consume(GREATER, "Expected '>' after type parameter.")
					}
					types.add(TypeToken(additionalToken, additionalTypeParameter, false))
				}
				if(additionalPureTypeParameter) {
					types.add(TypeToken(additionalToken, listOf(), true))
				}
			} else {
				break
			}
		} while (match(PIPE))
		return types
	}

	private fun getType(function: Boolean = false, noColon: Boolean = false): Type {
		var type: Type = if(function) Type.NIL else Type.ANY
		if (match(COLON) || noColon) {
			type = Type.of(getTypeTokens(false))
		}
		return type
	}

	private fun expressionStatement(): Stmt {
		val expr = expression()
		consume(SEMICOLON, "Expected ';' after expression.")
		return Stmt.Expression(expr)
	}

	private fun block(): List<Stmt> {
		val statements: MutableList<Stmt> = ArrayList()

		while (!checkType(RIGHT_BRACE) && !isAtEnd()) {
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

		when {
			match(EQUAL) -> {
				val equals = previous()
				val value = assignment()

				if (expr is Variable) {
					val name = expr.name
					return Assign(name, value, EQUAL, expr.getExprType())
				} else if (expr is Get) {
					return Set(expr.obj, expr.name, value, EQUAL, expr.getExprType())
				} else if(expr is ArrayGet) {
					return ArraySet(expr.obj, expr.what, value, previous(), EQUAL, expr.getExprType())
				}

				error(equals, "Invalid assignment target.")
			}
			match(PLUS_EQUAL) -> {
				val equals = previous()
				val value = assignment()

				if (expr is Variable) {
					val name = expr.name
					return Assign(name, value, PLUS_EQUAL, expr.getExprType())
				} else if (expr is Get) {
					return Set(expr.obj, expr.name, value, PLUS_EQUAL, expr.getExprType())
				} else if(expr is ArrayGet) {
					return ArraySet(expr.obj, expr.what, value, previous(), PLUS_EQUAL, expr.getExprType())
				}

				error(equals, "Invalid assignment target.")
			}
			match(MINUS_EQUAL) -> {
				val equals = previous()
				val value = assignment()

				if (expr is Variable) {
					val name = expr.name
					return Assign(name, value, MINUS_EQUAL, expr.getExprType())
				} else if (expr is Get) {
					return Set(expr.obj, expr.name, value, MINUS_EQUAL, expr.getExprType())
				} else if(expr is ArrayGet) {
					return ArraySet(expr.obj, expr.what, value, previous(), MINUS_EQUAL, expr.getExprType())
				}

				error(equals, "Invalid assignment target.")
			}
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
		var expr = check()

		while (match(AND)) {
			val operator = previous()
			val right = check()
			expr = Logical(expr, operator, right)
		}

		return expr
	}

	private fun check(): Expr {
		var expr = cast()

		if(match(IS)) {
			val operator = previous()
			val right = getType(function = false, noColon = true)
			expr = Check(expr, operator, right)
		} else if(match(IS_NOT)){
			val operator = previous()
			val right = getType(function = false, noColon = true)
			expr = Check(expr, operator, right)
		}

		return expr
	}

	private fun cast(): Expr {
		var expr = equality()

		if(match(AS)) {
			val operator = previous()
			val right = getType(function = false, noColon = true)
			expr = Cast(expr, operator, right)
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
		if (match(BANG, MINUS/*, INCREMENT, DECREMENT*/)) {
			val operator = previous()
			val right = unary()
			return Unary(operator, right)
		}

		return lambda()
	}

	private fun lambda(): Expr {
		val token = peek()
		if(match(FUN)){
			currentFunction = previous()
			//while (true) {

			val typeParameters: MutableList<Param> = ArrayList()
			if(match(LESS)){
				do {
					if (typeParameters.size >= 255) {
						error(peek(), "Can't have more than 255 type parameters.")
					}

					val identifier = consume(IDENTIFIER, "Expected type parameter name.")
					typeParameters.add(Param(identifier, Type.Parameter(identifier)))
				} while (match(COMMA))
				consume(GREATER, "Expected '>' after type parameter declaration.")
			}

			consume(LEFT_PAREN, "Expected '(' after lambda declaration")

			return finishLambda(token, typeParameters)
			//}
		}
		return call()
	}

	private fun call(): Expr {
		var expr = primary()

		while (true) {
			if (match(LEFT_PAREN)) {

				val typeParameters: MutableList<Type> = ArrayList()
				if(match(LESS)){
					do {
						if (typeParameters.size >= 255) {
							error(peek(), "Can't have more than 255 type parameters.")
						}

						typeParameters.add(getType(function = false, noColon = true))
					} while (match(COMMA))
					consume(GREATER, "Expected '>' after type parameter declaration.")
				}

				expr = finishCall(expr, typeParameters)
			} else if (match(DOT)) {
				val name: Token = consume(IDENTIFIER, "Expected expression after '.'.")
				if(sunlite.collector != null){
					val type = sunlite.collector?.findType(name, Token.identifier(expr.getExprType().getName(),-1,currentFile))?.getElementType() ?: Type.UNKNOWN
					expr = Get(expr, name, type)
				} else {
					expr = Get(expr, name)
				}
			} else if(match(LEFT_BRACKET)) {
				val name = expression()
				consume(RIGHT_BRACKET, "Expected ']' after expression.")
				expr = ArrayGet(expr, name, previous())
			} else {
				break
			}
		}

		return expr
	}

	private fun finishLambda(token: Token, typeParameters: MutableList<Param>): Expr {
		val parameters: MutableList<Param> = ArrayList()
		if (!checkType(RIGHT_PAREN)) {
			do {
				if (parameters.size >= 255) {
					error(peek(), "Can't have more than 255 parameters.")
				}

				parameters.add(
					Param(consume(IDENTIFIER, "Expected parameter name."),getType())
				)
			} while (match(COMMA))
		}
		consume(RIGHT_PAREN, "Expected ')' after parameters.")

		val type = getType(function = true)

		consume(LEFT_BRACE, "Expected '{' before lambda body.")
		val body = block()

		currentFunction = null

		return Lambda(
			Stmt.Function(
				Token(
					IDENTIFIER,
					"<lambda ${System.currentTimeMillis()}>",
					null,
					token.line,
					currentFile,
					token.pos
				), FunctionType.LAMBDA, parameters, body, FunctionModifier.NORMAL, type, typeParameters
			)
		)
	}

	private fun finishCall(callee: Expr, typeArguments: List<Type>): Expr {
		val arguments: MutableList<Expr> = ArrayList()
		if (!checkType(RIGHT_PAREN)) {
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

		return Call(callee, paren, arguments, typeArguments)
	}

	private fun primary(): Expr {
		if (match(FALSE)) return Literal(false,previous().line,previous().file, Type.BOOLEAN)
		if (match(TRUE)) return Literal(true,previous().line,previous().file, Type.BOOLEAN)
		if (match(NIL)) return Literal(null,previous().line,previous().file, Type.NIL)

		if (match(NUMBER, STRING)) {
			if(previous().type == NUMBER){
				return Literal(previous().literal,previous().line,previous().file, Type.NUMBER)
			} else {
				return Literal(previous().literal,previous().line,previous().file, Type.STRING)
			}
		}

		if (match(SUPER)) {
			val keyword = previous()
			consume(DOT, "Expected '.' after 'super'.")
			if(!match(IDENTIFIER, INIT)){
				throw error(peek(), "Expected superclass method name.")
			}
			val method = previous()
			if(sunlite.collector != null){
				val type = sunlite.collector?.findType(Token.identifier("<superclass>",-1, keyword.file), currentClass)?.getElementType() ?: Type.UNKNOWN
				val methodType = sunlite.collector?.findType(Token.identifier(method.lexeme,-1, keyword.file), Token.identifier(type.getName(),-1,keyword.file))?.getElementType() ?: Type.UNKNOWN
				return Super(keyword, method, methodType)
			} else {
				return Super(keyword, method)
			}
		}

		if (match(THIS)) {
            sunlite.collector?.let {
                val type = currentClass?.let {
                    sunlite.collector?.findType(currentClass!!, Token.identifier(currentFile ?: "<global>",-1,currentFile))
                }
				return This(previous(), (type?.getElementType() as Type.Reference?)?.returnType ?: Type.UNKNOWN)
            }
			return This(previous())
		}

		if (match(LEFT_PAREN)) {
			val expr = expression()
			consume(RIGHT_PAREN, "Expected ')' after expression.")
			return Grouping(expr)
		}

		if (match(IDENTIFIER)) {
			val varToken = previous()
			if(sunlite.collector != null){
				val type = sunlite.collector?.findType(varToken, Token.identifier(currentFile ?: "<global>",-1,currentFile))
				return Variable(varToken, type?.getElementType() ?: Type.UNKNOWN)
			}
			return Variable(varToken)
		}

		throw error(peek(), "Expected expression.")
	}

	private fun consume(type: TokenType, message: String): Token {
		if (checkType(type)) return advance()

		throw error(peek(), message)
	}

	private fun assert(type: TokenType, message: String): Token {
		if (!checkType(type)) return peek()

		throw error(peek(), message)
	}

	private fun match(vararg types: TokenType): Boolean {
		for (type in types) {
			if (checkType(type)) {
				advance()
				return true
			}
		}

		return false
	}

	private fun checkNext(vararg types: TokenType): Boolean {
		for (type in types) {
			if (checkNext(type)) {
				return true
			}
		}

		return false
	}

	private fun checkType(vararg types: TokenType): Boolean {
		for (type in types) {
			if (checkType(type)) {
				return true
			}
		}

		return false
	}

	private fun checkType(type: TokenType): Boolean {
		if (isAtEnd()) return false
		return peek().type === type
	}

	private fun checkNext(type: TokenType): Boolean {
		if (isAtEnd()) return false
		return next().type === type
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

	private fun next(): Token {
		if(current + 1 >= tokens.size) throw error(peek(), "Unexpected end of file.")
		return tokens[current + 1]
	}

	private fun error(token: Token, message: String): ParseError {
		sunlite.error(token, message)
		return ParseError()
	}

	private fun synchronize() {
		advance()

		while (!isAtEnd()) {
			if (previous().type == SEMICOLON) return

			when (peek().type) {
				CLASS, INTERFACE, FUN, VAR, FOR, IF, WHILE, /*PRINT,*/ RETURN, BREAK, CONTINUE -> return
				else -> {}
			}

			advance()
		}
	}


}