package sunsetsatellite.sunlite.lang

import sunsetsatellite.sunlite.lang.Expr.*
import sunsetsatellite.sunlite.lang.Expr.Set
import sunsetsatellite.sunlite.lang.TokenType.*
import java.io.IOException


class Parser(
    val tokens: List<Token>,
    val sunlite: Sunlite,
    val allowIncluding: Boolean = false,
    val including: Boolean = false,
    val includingDepth: Int = 0,
    val importing: String = "",
) {
    private var current = 0

    var currentPackage: String = "default"
    var currentFile: String? = null
    var currentClass: Token? = null
    var currentFunction: Token? = null
    var currentBlockDepth: Int = 0
    var lambdaAmount = 0

    val annotations: MutableList<Stmt.Annotation> = mutableListOf()

    companion object {
        val autoImported: MutableMap<String, String> = mutableMapOf()
        init {
            autoImported["Object"] = "/sunlite/stdlib/object.sl"
            autoImported["Exception"] = "/sunlite/stdlib/exception.sl"
            autoImported["ArrayIterator"] = "/sunlite/stdlib/array.sl"
            autoImported["array"] = "/sunlite/stdlib/array.sl"
            autoImported["string"] = "/sunlite/stdlib/string.sl"
        }
    }

    private class ParseError : RuntimeException()

    fun parse(path: String?): List<Stmt> {
        currentFile = path
        val statements: MutableList<Stmt> = ArrayList()

        if(!isAtEnd()){
            pckg()?.let { statements.add(it) }
        }

        if(autoImported.values.none { it == path }){
            autoImported.forEach { (name, path) ->
                doImport(
	                location = Token(STRING, "\"$path\"", path, -1, currentFile, Token.Position(-1,-1)),
	                what = Token.identifier(name, -1, currentFile),
	                keyword = Token.unknown(),
	                alias = null
                )
            }
        }

        while (!isAtEnd()) {
            annotation()?.let { statements.add(it) }
        }

        return statements
    }

    private fun pckg(): Stmt? {
        try {
            return when {
                match(PACKAGE) -> packageDeclaration()
                else -> annotation()
            }
        } catch (error: ParseError) {
            synchronize()
            return null
        }
    }

    private fun annotation(): Stmt? {
        try {
            return when {
                match(AT) -> annotationDeclaration()
                else -> declaration()
            }
        } catch (error: ParseError) {
            synchronize()
            return null
        }
    }

    private fun allowedToParse(): Boolean = (importing.isEmpty() || importing.isNotEmpty() && importing == currentClass?.lexeme) || currentClass != null

    private fun declaration(): Stmt? {
        return when {
            match(INCLUDE) -> includeStatement()
            match(IMPORT) -> importStatement()
            match(VAR) -> {
                val decl = varDeclaration()
                if(allowedToParse()) decl else null
            }
            match(VAL) -> {
                val decl = varDeclaration(FieldModifier.CONST)
                if(allowedToParse()) decl else null
            }
            match(FUN) -> {
                val decl = function(FunctionType.FUNCTION, null)
                if(allowedToParse()) decl else null
            }
            match(ABSTRACT) -> {
                consume(CLASS, "Expected 'class' after class modifier.")
                classDeclaration(ClassModifier.ABSTRACT)
            }
            match(CLASS) -> classDeclaration(ClassModifier.NORMAL)
            match(INTERFACE) -> interfaceDeclaration()
            else -> {
                val stmt = statement()
                if(allowedToParse()) stmt else null
            }
        }
    }

    private fun statement(): Stmt {
        return when {
            match(THROW) -> throwStatement()
            match(LEFT_BRACE) -> Stmt.Block(block(), previous().line, previous().file)
            match(IF) -> ifStatement()
            match(WHILE) -> whileStatement()
            match(MATCH) -> matchStatement()
            match(FOR) -> forStatement()
            match(FOREACH) -> foreachStatement()
            match(BREAK) -> breakStatement()
            match(CONTINUE) -> continueStatement()
            match(RETURN) -> returnStatement()
            match(TRY) -> tryCatchStatement()
            else -> expressionStatement()
        }
    }

    private fun packageDeclaration(): Stmt {
        val keyword = previous()
        var what: Token
        what = consume(STRING, "Expected package name.")
        consume(SEMICOLON, "Expected ';' after package statement.")
        currentPackage = what.literal as String
        return Stmt.Package(keyword, what)
    }

    private fun importStatement(): Stmt? {
        val keyword = previous()
        val what = consume(IDENTIFIER, "Expected class name.")
        consume(FROM, "Expected 'from' after class name of import statement.")
        val location: Token = consume(STRING, "Expected import location string.")
        var alias: Token? = null
        if(match(AS)){
            alias = consume(IDENTIFIER, "Expected alias name.")
        }
        consume(SEMICOLON, "Expected ';' after import statement.")
        return doImport(location, what, keyword, alias)
    }

    private fun doImport(
        location: Token,
        what: Token,
        keyword: Token,
        alias: Token?
    ): Stmt.Import? {
        val id = (location.literal as String) + "::" + what.lexeme
        if (sunlite.imports.contains(id)) {
            if (sunlite.imports[id]?.second == null) {
                //sunlite.error(keyword, "ImportError: Circular import detected.")
                return null
            }
            return Stmt.Import(keyword, what, location, alias)
        }

        sunlite.imports[id] = includingDepth to null

        if (sunlite.collector == null || !allowIncluding) {
            return Stmt.Import(keyword, what, location, alias)
        }

        var data: String? = null
        val invalidPaths: MutableList<String> = mutableListOf()

        data = Sunlite::class.java.getResourceAsStream(location.literal)?.bufferedReader()?.use { it.readText() }

        if (data == null) {
            sunlite.path.forEach {
                try {
                    data = sunlite.readFunction.apply(it + location.literal)
                } catch (_: IOException) {
                    invalidPaths.add(it)
                }
            }
        }

        if (data == null) {
            sunlite.imports.remove(id)
            sunlite.error(keyword, "ImportError: Couldn't find '${location.literal}' on the load path list.")
            return null
        }

        val scanner = Scanner(data, sunlite)
        val tokens: List<Token> = scanner.scanTokens(location.literal)

        var parser = Parser(tokens, sunlite, true, true, includingDepth + 1, what.lexeme)
        var statements = parser.parse(location.literal)

        // Stop if there was a syntax error.
        if (sunlite.hadError) {
            sunlite.imports.remove(id)
            sunlite.error(keyword, "ImportError: SyntaxError in file being imported.")
            return null
        }

        sunlite.collector?.collect(statements, location.literal, sunlite.compileStep)

        // Stop if there was a type collection error.
        if (sunlite.hadError) {
            sunlite.imports.remove(id)
            sunlite.error(keyword, "ImportError: TypeError in file being imported.")
            return null
        }

        parser = Parser(tokens, sunlite, true, true, includingDepth + 1, what.lexeme)
        statements = parser.parse(location.literal)

        // Stop if there was a syntax error.
        if (sunlite.hadError) {
            sunlite.imports.remove(id)
            sunlite.error(keyword, "ImportError: SyntaxError in file being imported.")
            return null
        }

        sunlite.collector?.collect(statements, location.literal, sunlite.compileStep + 2)

        if(sunlite.compileStep > 0){
            val checker = TypeChecker(sunlite, null)
            checker.check(statements)
        }

        // Stop if there was a type error.
        if (sunlite.hadError) {
            sunlite.imports.remove(id)
            sunlite.error(keyword, "ImportError: TypeError in file being imported.")
            return null
        }

        sunlite.imports[id] = includingDepth to statements

        if (Sunlite.showAST) {
            sunlite.printInfo("AST: ${location.literal}")
            sunlite.printInfo("-----")
            statements.forEach {
                sunlite.printInfo(AstPrinter.print(it))
            }
            sunlite.printInfo("-----")
            sunlite.printInfo()
        }

        if (Sunlite.debug) {
            sunlite.printInfo("Parsed and imported ${what.lexeme} from ${location.literal}!")
            sunlite.printInfo()
        }
        return Stmt.Import(keyword, what, location, alias)
    }

    private fun includeStatement(): Stmt? {
        val keyword = previous()
        var builtin: Boolean = false
        var what: Token
        if(match(LESS)){
            what = consume(STRING, "Expected builtin include location string.")
            consume(GREATER, "Expected '>' after include statement.")
            builtin = true
        } else {
            what = consume(STRING, "Expected include location string.")
        }
        consume(SEMICOLON, "Expected ';' after include statement.")

        if (sunlite.includes.contains(what.literal as String)) {
            return null
        }

        if (sunlite.collector == null || !allowIncluding) {
            return null
        }

        var data: String? = null
        val invalidPaths: MutableList<String> = mutableListOf()

        if(!builtin){
            sunlite.path.forEach {
                try {
                    data = sunlite.readFunction.apply(it + what.literal)
                } catch (_: IOException) {
                    invalidPaths.add(it)
                }
            }
        } else {
            data = Sunlite::class.java.getResourceAsStream(what.literal)?.bufferedReader()?.use { it.readText() }
        }


        if (data == null) {
            sunlite.error(keyword, "ImportError: Couldn't find '${what.literal}' on the load path list.")
            return null
        }

        val scanner = Scanner(data, sunlite)
        val tokens: List<Token> = scanner.scanTokens(what.literal)

        var parser = Parser(tokens, sunlite, true, true, includingDepth + 1)
        var statements = parser.parse(what.literal)

        // Stop if there was a syntax error.
        if (sunlite.hadError) {
            sunlite.error(keyword, "ImportError: SyntaxError in file being imported.")
            return null
        }

        sunlite.collector?.collect(statements, what.literal, sunlite.compileStep)

        // Stop if there was a type collection error.
        if (sunlite.hadError) {
            sunlite.error(keyword, "ImportError: TypeError in file being imported.")
            return null
        }

        parser = Parser(tokens, sunlite, false, true, includingDepth + 1)
        statements = parser.parse(what.literal)

        // Stop if there was a syntax error.
        if (sunlite.hadError) {
            sunlite.error(keyword, "ImportError: SyntaxError in file being imported.")
            return null
        }

        val checker = TypeChecker(sunlite, null)
        checker.check(statements)

        // Stop if there was a type error.
        if (sunlite.hadError) {
            sunlite.error(keyword, "ImportError: TypeError in file being imported.")
            return null
        }

        sunlite.includes[what.literal] = includingDepth to statements

        if (Sunlite.showAST) {
            sunlite.printInfo("AST: ${currentFile}")
            sunlite.printInfo("-----")
            statements.forEach {
                sunlite.printInfo(AstPrinter.print(it))
            }
            sunlite.printInfo("-----")
            sunlite.printInfo()
        }

        if (Sunlite.debug) {
            sunlite.printInfo("Parsed and included '${what.literal}'!")
            sunlite.printInfo()
        }

        return Stmt.Include(keyword, what)
    }

    private fun classDeclaration(modifier: ClassModifier): Stmt? {
        //if(modifier == ClassModifier.DYNAMIC) consume(CLASS, "Expected 'class' after class modifier.")

        val typeParameters: MutableList<Param> = ArrayList()
        if (match(LESS)) {
            //throw error(peek(), "Generic classes not supported.")
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
        if(superclass == null && name.lexeme != "Object"){
            superclass = Variable(Token.identifier("Object", previous()))
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

        if(importing.isEmpty() || (importing.isNotEmpty() && name.lexeme == importing)){
            val types = sunlite.collector?.typeHierarchy
            if(types?.containsKey(name.lexeme) == false || (types?.containsKey(name.lexeme) == true && types[name.lexeme]?.incomplete == true)){
	            types[name.lexeme] = TypeCollector.TypePrototype(
                    name.lexeme,
                    superclass?.name?.lexeme ?: "<nil>",
                    superinterfaces.map { it.name.lexeme },
                    typeParameters.map { it.token.lexeme },
                    modifier,
                    null,
                    true,
                    sunlite.compileStep
                )
            }
        }


        val methods: MutableList<Stmt.Function> = ArrayList()
        val fields: MutableList<Stmt.Var> = ArrayList()
        while (!checkToken(RIGHT_BRACE) && !isAtEnd()) {
            val currentModifier = peek()
            when {
                match(OPERATOR) -> {
                    when {
                        match(FUN) -> {
                            methods.add(function(FunctionType.METHOD, currentModifier))
                        }
                    }
                }

                checkToken(OVERRIDE) && checkNext(REQUIRED) -> {
                    val modifier = peek()
                    val modifier2 = next()
                    advance()
                    advance()
                    when {
                        match(FUN) -> {
                            methods.add(function(FunctionType.METHOD, modifier, modifier2))
                        }
                    }
                }

                match(ABSTRACT) -> {
                    methods.add(abstractMethod())
                }

                match(OVERRIDE) -> {
                    when {
                        match(FUN) -> {
                            methods.add(function(FunctionType.METHOD, currentModifier))
                        }
                    }
                }

                match(REQUIRED) -> {
                    when {
                        match(FUN) -> {
                            methods.add(function(FunctionType.METHOD, currentModifier))
                        }
                    }
                }

                checkToken(STATIC) && checkNext(NATIVE) -> {
                    val modifier = peek()
                    val modifier2 = next()
                    advance()
                    advance()
                    when {
                        match(FUN) -> {
                            methods.add(function(FunctionType.METHOD, modifier, modifier2))
                        }
                    }
                }

                match(STATIC) || match(NATIVE) -> {
                    if (previous().type == STATIC) {
                        when {
                            match(VAR) -> {
                                fields.add(varDeclaration(FieldModifier.STATIC))
                            }

                            match(VAL) -> {
                                fields.add(varDeclaration(FieldModifier.STATIC_CONST))
                            }

                            match(FUN) -> {
                                methods.add(function(FunctionType.METHOD, currentModifier))
                            }

                            else -> {
                                throw error(peek(), "Expected a field or method declaration.")
                            }
                        }
                    } else if (match(FUN)) {
                        throw error(peek(), "Expected 'static' before native method declaration.")
                        //methods.add(function(FunctionType.METHOD, currentModifier))
                    } else {
                        throw error(peek(), "Expected a field or method declaration.")
                    }
                }

                match(VAR) -> {
                    fields.add(varDeclaration())
                }

                match(VAL) -> {
                    fields.add(varDeclaration(FieldModifier.CONST))
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

        if(importing.isNotEmpty() && name.lexeme != importing) return null

        return Stmt.Class(name, methods, fields, superclass, superinterfaces, modifier, typeParameters)
    }

    private fun interfaceDeclaration(): Stmt? {

        val typeParameters: MutableList<Param> = ArrayList()
        if (match(LESS)) {
			do {
				if (typeParameters.size >= 255) {
					error(peek(), "Can't have more than 255 type parameters.")
				}

				val identifier = consume(IDENTIFIER, "Expected type parameter name.")
				typeParameters.add(Param(identifier, Type.Parameter(identifier)))
			} while (match(COMMA))
			consume(GREATER, "Expected '>' after type parameter declaration.")
        }

        val name = consume(IDENTIFIER, "Expected interface name.")

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

        consume(LEFT_BRACE, "Expected '{' before interface body.")

        if(importing.isEmpty() || (importing.isNotEmpty() && name.lexeme == importing)){
            val types = sunlite.collector?.typeHierarchy
            if(types?.containsKey(name.lexeme) == false || (types?.containsKey(name.lexeme) == true && types[name.lexeme]?.incomplete == true)){
                types[name.lexeme] = TypeCollector.TypePrototype(
                    name.lexeme,
                    "<nil>",
                    superinterfaces.map { it.name.lexeme },
                    typeParameters.map { it.token.lexeme },
                    ClassModifier.ABSTRACT,
                    null,
                    true,
                    sunlite.compileStep
                )
            }
        }

        val methods: MutableList<Stmt.Function> = ArrayList()
        while (!checkToken(RIGHT_BRACE) && !isAtEnd()) {
            methods.add(abstractMethod())
        }

        consume(RIGHT_BRACE, "Expected '}' after interface body.")

        if(importing.isNotEmpty() && name.lexeme != importing) return null

        return Stmt.Interface(name, methods, superinterfaces, typeParameters)
    }

    private fun funcSignature(kind: FunctionType): Triple<Token, List<Param>, Type> {
        var name = if (kind == FunctionType.INITIALIZER) Token.identifier("init", -1, currentFile) else consume(
            IDENTIFIER,
            "Expected ${kind.toString().lowercase()} name."
        )

        consume(LEFT_PAREN, "Expected '(' after ${kind.toString().lowercase()} name.")
        val parameters: MutableList<Param> = ArrayList()
        if (!checkToken(RIGHT_PAREN)) {
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

        if (kind == FunctionType.INITIALIZER) {
            val desc = "init"+Type.ofFunction("init", Type.NIL, parameters).getDescriptor()
            name = Token.identifier(desc, -1, currentFile)
        }

        currentFunction = name
        return Triple(name, parameters, type)
    }

    private fun abstractMethod(): Stmt.Function {
        consume(FUN, "Expected abstract method declaration")

        val typeParameters: MutableList<Param> = ArrayList()
        if (match(LESS)) {
            //throw error(peek(), "Generic functions not supported.")
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
            arrayOf(FunctionModifier.ABSTRACT),
            signature.third,
            typeParameters
        )
    }

    private fun function(kind: FunctionType, modifier: Token?, modifier2: Token? = null): Stmt.Function {
        if (modifier != null && FunctionModifier.entries.none { it.name.lowercase() == modifier.type.name.lowercase() }) {
            error(peek(), "Invalid ${kind.toString().lowercase()} modifier '${modifier.lexeme}'.")
        }

        val typeParameters: MutableList<Param> = ArrayList()
        if (match(LESS)) {
            //throw error(peek(), "Generic functions not supported.")
			do {
				if (typeParameters.size >= 255) {
					error(peek(), "Can't have more than 255 type parameters.")
				}

				val identifier = consume(IDENTIFIER, "Expected type parameter name.")
				typeParameters.add(Param(identifier, Type.Parameter(identifier)))
			} while (match(COMMA))
			consume(GREATER, "Expected '>' after type parameter declaration.")
        }

        val funcModifier = FunctionModifier.get(modifier, modifier2)
        val signature = funcSignature(kind)

        var body: List<Stmt> = listOf()
        if (!funcModifier.contains(FunctionModifier.NATIVE)) {
            consume(LEFT_BRACE, "Expected '{' before ${kind.toString().lowercase()} body.")
            body = block()
        } else {
            assert(LEFT_BRACE, "Native ${kind.toString().lowercase()} cannot have a body.")
        }

        currentFunction = null

        val ann = ArrayList<Stmt.Annotation>(annotations)
        annotations.clear()

        return Stmt.Function(
            signature.first,
            kind,
            signature.second,
            body,
            funcModifier,
            signature.third,
            typeParameters,
            ann
        )
    }

    private fun tryCatchStatement(): Stmt {
        val tryToken = previous()
        consume(LEFT_BRACE, "Expected '{' before try block.")
        val tryBlock = block()
        consume(CATCH, "Expected 'catch' after try block.")
        val catchToken = previous()
        consume(LEFT_PAREN, "Expected '(' after 'catch'.")
        var type = getType()
        if(type == Type.ANY){
            type = Type.ofObject("Exception")
        }
        val catchVariable = Param(consume(IDENTIFIER, "Expected catch variable name."), type)
        consume(RIGHT_PAREN, "Expected ')' after catch variable.")
        consume(LEFT_BRACE, "Expected '{' before catch block.")
        val catchBlock = block()
        return Stmt.TryCatch(
            tryToken,
            catchToken,
            Stmt.Block(tryBlock, previous().line, previous().file),
            catchVariable,
            Stmt.Block(catchBlock, previous().line, previous().file)
        )
    }

    private fun returnStatement(): Stmt {
        val keyword = previous()
        var value: Expr? = null
        if (!checkToken(SEMICOLON)) {
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
        throw error(
            peek(),
            "Print as a statement has been deprecated, please use the global function 'print(...)' instead."
        )
        /*val value = expression()
        consume(SEMICOLON, "Expected ';' after value.")
        return Stmt.Print(value)*/
    }

    private fun throwStatement(): Stmt {
        val keyword = previous()
        val value = expression()
        consume(SEMICOLON, "Expected ';' after expression.")
        return Stmt.Throw(keyword, value)
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
        if (!checkToken(SEMICOLON)) {
            condition = expression()
        }
        consume(SEMICOLON, "Expected ';' after loop condition.")

        var increment: Expr? = null
        if (!checkToken(RIGHT_PAREN)) {
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

        if (condition == null) condition = Literal(true, peek().line, peek().file)
        body = Stmt.While(condition, body)

        if (initializer != null) {
            body = Stmt.Block(listOf(initializer, body), peek().line, peek().file)
        }

        //inLoop = false

        return body
    }

    private fun foreachStatement(): Stmt {
        consume(LEFT_PAREN, "Expected '(' after 'foreach'.")
        consume(VAR, "Expected variable declaration for 'foreach' loop.")
        var element = varDeclaration(FieldModifier.NORMAL, true)
        consume(IN, "Expected 'in' after variable declaration for 'foreach' loop.")
        val collection = expression()
        consume(RIGHT_PAREN, "Expected ')' after 'foreach' clauses.")
        val initCall = Call(
            Get(collection, Token.identifier("getIterator", collection), Type.ofObject("Iterator")),
            Token.identifier("<synthetic iterator init call>", collection),
            listOf(),
            listOf()
        )
        val initializer = Stmt.Var(
            Token.identifier("<iter>", collection),
            Type.ofObject("Iterator"), initCall, FieldModifier.NORMAL
        )
        val iterVar = Variable(Token.identifier("<iter>", collection), Type.ofObject("Iterator"))
        val nextCall = Call(
            Get(iterVar, Token.identifier("hasNext", collection), Type.ofFunction("hasNext", Type.BOOLEAN, listOf())),
            Token.identifier("<synthetic iterator hasNext call>", collection),
            listOf(),
            listOf()
        )
        val condition = Binary(
            nextCall,
            Token(
                EQUAL_EQUAL,
                "==",
                null,
                collection.getLine(),
                collection.getFile(),
                Token.Position(-1, -1)
            ),
            Literal(true, collection.getLine(), collection.getFile(), Type.BOOLEAN)
        )
        val increment = Call(
            Get(
                iterVar,
                Token.identifier("next", collection),
                Type.ofFunction("next", /*todo*/Type.NULLABLE_ANY, listOf())
            ),
            Token.identifier("<synthetic iterator next call>", collection),
            listOf(),
            listOf()
        )
        element = Stmt.Var(
            element.name, element.type,
            Call(
                Get(
                    iterVar,
                    Token.identifier("current", element.name),/*todo*/
                    Type.ofFunction("current", Type.NULLABLE_ANY, listOf())
                ),
                Token.identifier("<synthetic iterator current call>", element.name),
                listOf(),
                listOf(),
            ), FieldModifier.NORMAL
        )

        var body = statement()

        body = Stmt.Block(listOf(element, body), peek().line, peek().file)
        body = Stmt.Block(listOf(body, Stmt.Expression(increment)), peek().line, peek().file)
        body = Stmt.While(condition, body)
        body = Stmt.Block(listOf(initializer, body), peek().line, peek().file)

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

    private fun matchStatement(): Stmt {
        consume(LEFT_PAREN, "Expected '(' after 'match'.")
        val matchVariable = consume(IDENTIFIER, "Expected match variable name.")
        consume(RIGHT_PAREN, "Expected ')' after 'match'.")
        consume(LEFT_BRACE, "Expected '{' after 'match(...)'.")
        //

        val cases: MutableList<Pair<List<Expr>, List<Stmt>>> = mutableListOf()
        var elseCase: List<Stmt>? = null
        while (!checkToken(RIGHT_BRACE) && !isAtEnd()) {
            if (match(ELSE)) {
                if (elseCase != null) throw error(peek(), "Can't have multiple 'else' cases in a 'match' statement.")
                consume(COLON, "Expected ':' after match case.")
                consume(LEFT_BRACE, "Expected '{' after ':' in match case.")
                elseCase = block()
                continue
            }
            val matchers: MutableList<Expr> = mutableListOf()
            do {
                matchers.add(expression())
            } while (match(COMMA))
            consume(COLON, "Expected ':' after match case.")
            consume(LEFT_BRACE, "Expected '{' after ':' in match case.")
            val block = block()
            cases.add(Pair(matchers, block))
        }

        consume(RIGHT_BRACE, "Expected '}' after 'match' statement.")

        if (cases.isEmpty()) {
            throw error(peek(), "Expected at least one match case in 'match' statement.")
        }

        val matchers = cases.last().first
        var condition: Expr
        if (matchers.size < 2) {
            condition = Binary(
                Variable(matchVariable),
                Token(EQUAL_EQUAL, "==", null, peek().line, peek().file, Token.Position(-1, -1)),
                matchers.first()
            )
        } else {
            condition = Logical(
                Binary(
                    Variable(matchVariable),
                    Token(EQUAL_EQUAL, "==", null, peek().line, peek().file, Token.Position(-1, -1)),
                    matchers[0]
                ),
                Token(OR, "or", null, peek().line, peek().file, Token.Position(-1, -1)),
                Binary(
                    Variable(matchVariable),
                    Token(EQUAL_EQUAL, "==", null, peek().line, peek().file, Token.Position(-1, -1)),
                    matchers[1]
                ),
            )
            if (matchers.size > 2) {
                for (i in 2 until matchers.size) {
                    condition = Logical(
                        condition,
                        Token(OR, "or", null, peek().line, peek().file, Token.Position(-1, -1)),
                        Binary(
                            Variable(matchVariable),
                            Token(EQUAL_EQUAL, "==", null, peek().line, peek().file, Token.Position(-1, -1)),
                            matchers[i]
                        ),
                    )
                }
            }
        }


        var stmt: Stmt = Stmt.If(
            condition,
            Stmt.Block(cases.last().second, peek().line, peek().file),
            Stmt.Block(elseCase ?: listOf(), peek().line, peek().file)
        )

        cases.reversed().slice(1..<cases.size).forEach {

            val matchers = it.first
            var condition: Expr
            if (matchers.size < 2) {
                condition = Binary(
                    Variable(matchVariable),
                    Token(EQUAL_EQUAL, "==", null, peek().line, peek().file, Token.Position(-1, -1)),
                    matchers.first()
                )
            } else {
                condition = Logical(
                    Binary(
                        Variable(matchVariable),
                        Token(EQUAL_EQUAL, "==", null, peek().line, peek().file, Token.Position(-1, -1)),
                        matchers[0]
                    ),
                    Token(OR, "or", null, peek().line, peek().file, Token.Position(-1, -1)),
                    Binary(
                        Variable(matchVariable),
                        Token(EQUAL_EQUAL, "==", null, peek().line, peek().file, Token.Position(-1, -1)),
                        matchers[1]
                    ),
                )
                if (matchers.size > 2) {
                    for (i in 2 until matchers.size) {
                        condition = Logical(
                            condition,
                            Token(OR, "or", null, peek().line, peek().file, Token.Position(-1, -1)),
                            Binary(
                                Variable(matchVariable),
                                Token(EQUAL_EQUAL, "==", null, peek().line, peek().file, Token.Position(-1, -1)),
                                matchers[i]
                            ),
                        )
                    }
                }
            }

            stmt = Stmt.If(
                condition,
                Stmt.Block(it.second, peek().line, peek().file),
                stmt
            )
        }

        return stmt
    }

    private fun annotationDeclaration(): Stmt? {
        val name = consume(IDENTIFIER, "Expected variable name.")
        annotations.add(Stmt.Annotation(name))
        return when {
            match(AT) -> annotationDeclaration()
            //match(VAR) -> varDeclaration()
            //match(VAL) -> varDeclaration(FieldModifier.CONST)
            match(FUN) -> function(FunctionType.FUNCTION, null)
            //match(CLASS) -> classDeclaration(ClassModifier.NORMAL)
            //match(INTERFACE) -> interfaceDeclaration()
            else -> throw error(previous(), "Expected annotatable declaration after annotation.")
        }
    }

    private fun varDeclaration(modifier: FieldModifier = FieldModifier.NORMAL, foreach: Boolean = false): Stmt.Var {
        val name = consume(IDENTIFIER, "Expected variable name.")

        var type = getType()

        var initializer: Expr? = null
        if (match(EQUAL)) {
            initializer = expression()
        }

        if(type == Type.UNKNOWN && initializer != null){
            type = initializer.getExprType()
        } else if(type == Type.UNKNOWN && initializer == null) {
            throw error(peek(), "Can't infer type of variable without an initializer.")
        }

        if (!foreach) consume(SEMICOLON, "Expected ';' after variable declaration.")
        return Stmt.Var(name, type, initializer, modifier)
    }

    private fun getTypeTokens(insideUnion: Boolean = false): List<TypeToken> {
        val mainToken = peek()
        if (!match(
                TYPE_BOOLEAN, TYPE_STRING, /*TYPE_NUMBER,*/
                TYPE_BYTE, TYPE_SHORT, TYPE_INT, TYPE_LONG, TYPE_FLOAT, TYPE_DOUBLE,
                TYPE_FUNCTION, TYPE_CLASS, TYPE_ANY, TYPE_GENERIC, TYPE_ARRAY, TYPE_TABLE, IDENTIFIER, TYPE_NIL
            )
        ) {
            throw error(mainToken, "Expected type.")
        }

        //there should be only one top most type (probably)
        val types: MutableList<TypeToken> = mutableListOf()

        val unionTypes: MutableMap<Token, List<TypeToken>> = mutableMapOf()
        val typeParameters: MutableList<TypeToken> = mutableListOf()

        if (match(LESS)) {
            do {
                if (typeParameters.size >= 255) {
                    error(peek(), "Can't have more than 255 type parameters.")
                }
                val typeParamToken = peek()
                if (!checkTokens(
                        TYPE_BOOLEAN,
                        TYPE_STRING, /*TYPE_NUMBER,*/
                        TYPE_BYTE,
                        TYPE_SHORT,
                        TYPE_INT,
                        TYPE_LONG,
                        TYPE_FLOAT,
                        TYPE_DOUBLE,
                        TYPE_FUNCTION,
                        TYPE_CLASS,
                        TYPE_ANY, TYPE_GENERIC,
                        TYPE_ARRAY,
                        TYPE_TABLE,
                        IDENTIFIER,
                        TYPE_NIL
                    )
                ) {
                    throw error(typeParamToken, "Expected type for type parameter.")
                }
                typeParameters.addAll(getTypeTokens(false))

            } while (match(COMMA))
            consume(GREATER, "Expected '>' after type parameters.")
        }

        if (checkToken(PIPE) && !insideUnion) {
            advance()
            do {
                if (unionTypes.size >= 255) {
                    error(peek(), "Can't have more than 255 types in a union.")
                }
                val unionMemberToken = peek()
                if (!checkTokens(
                        TYPE_BOOLEAN,
                        TYPE_STRING, /*TYPE_NUMBER,*/
                        TYPE_BYTE,
                        TYPE_SHORT,
                        TYPE_INT,
                        TYPE_LONG,
                        TYPE_FLOAT,
                        TYPE_DOUBLE,
                        TYPE_FUNCTION,
                        TYPE_CLASS,
                        TYPE_ANY, TYPE_GENERIC,
                        TYPE_ARRAY,
                        TYPE_TABLE,
                        IDENTIFIER,
                        TYPE_NIL
                    )
                ) {
                    throw error(unionMemberToken, "Expected type after '|'.")
                }
                val unionTypeTokens = getTypeTokens(true)
                unionTypes[unionMemberToken] = unionTypeTokens
            } while (match(PIPE))
        }

        if (match(QUESTION)) {
            val token = previous()
            unionTypes[token] = listOf(
                TypeToken(
                    mapOf(token to listOf(TypeToken(mapOf(token to listOf()), typeParameters))),
                    typeParameters
                )
            )
        }

        unionTypes[mainToken] = listOf(
            TypeToken(
                mapOf(
                    mainToken to listOf(
                        TypeToken(
                            mapOf(
                                mainToken to listOf()
                            ), typeParameters
                        )
                    )
                ), typeParameters
            )
        )

        types.add(TypeToken(unionTypes, typeParameters))

        return types
    }

    private fun getType(function: Boolean = false, noColon: Boolean = false): Type {
        var type: Type = if (function) Type.NIL else Type.ANY
        if (match(COLON) || noColon) {
            if(checkToken(EQUAL)){
                return Type.UNKNOWN
            }
            val typeTokens = getTypeTokens()
            type = Type.of(typeTokens, sunlite)
        }
        return type
    }

    private fun expressionStatement(): Stmt {
        val expr = expression()
        consume(SEMICOLON, "Expected ';' after expression.")
        return Stmt.Expression(expr)
    }

    private fun block(): List<Stmt> {
        currentBlockDepth++
        val statements: MutableList<Stmt> = ArrayList()

        while (!checkToken(RIGHT_BRACE) && !isAtEnd()) {
            declaration()?.let { statements.add(it) } ?: break
        }

        consume(RIGHT_BRACE, "Expected '}' after block.")
        currentBlockDepth--
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

                when (expr) {
                    is Variable -> {
                        if (expr.constant) {
                            throw error(equals, "Cannot reassign constant '${expr.name.lexeme}: ${expr.type}'.")
                        }
                        val name = expr.name
                        return Assign(name, value, EQUAL, expr.getExprType())
                    }

                    is Get -> {
                        if (expr.constant) {
                            throw error(equals, "Cannot reassign constant '${expr.name.lexeme}: ${expr.type}'.")
                        }
                        return Set(expr.obj, expr.name, value, EQUAL, expr.getExprType())
                    }

                    is ArrayGet -> {
                        return ArraySet(expr.obj, expr.what, value, previous(), EQUAL, expr.getExprType())
                    }

                    else -> error(equals, "Invalid assignment target.")
                }
            }

            match(PLUS_EQUAL) -> {
                val equals = previous()
                val value = assignment()

                when (expr) {
                    is Variable -> {
                        if (expr.constant) {
                            throw error(equals, "Cannot reassign constant '${expr.name.lexeme}: ${expr.type}'.")
                        }
                        val name = expr.name
                        return Assign(name, value, PLUS_EQUAL, expr.getExprType())
                    }

                    is Get -> {
                        if (expr.constant) {
                            throw error(equals, "Cannot reassign constant '${expr.name.lexeme}: ${expr.type}'.")
                        }
                        return Set(expr.obj, expr.name, value, PLUS_EQUAL, expr.getExprType())
                    }

                    is ArrayGet -> {
                        return ArraySet(expr.obj, expr.what, value, previous(), PLUS_EQUAL, expr.getExprType())
                    }

                    else -> error(equals, "Invalid assignment target.")
                }
            }

            match(MINUS_EQUAL) -> {
                val equals = previous()
                val value = assignment()

                when (expr) {
                    is Variable -> {
                        if (expr.constant) {
                            throw error(equals, "Cannot reassign constant '${expr.name.lexeme}: ${expr.type}'.")
                        }
                        val name = expr.name
                        return Assign(name, value, MINUS_EQUAL, expr.getExprType())
                    }

                    is Get -> {
                        if (expr.constant) {
                            throw error(equals, "Cannot reassign constant '${expr.name.lexeme}: ${expr.type}'.")
                        }
                        return Set(expr.obj, expr.name, value, MINUS_EQUAL, expr.getExprType())
                    }

                    is ArrayGet -> {
                        return ArraySet(expr.obj, expr.what, value, previous(), MINUS_EQUAL, expr.getExprType())
                    }

                    else -> error(equals, "Invalid assignment target.")
                }
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

        if (match(IS)) {
            val operator = previous()
            val right = getType(function = false, noColon = true)
            expr = Check(expr, operator, right)
        } else if (match(IS_NOT)) {
            val operator = previous()
            val right = getType(function = false, noColon = true)
            expr = Check(expr, operator, right)
        }

        return expr
    }

    private fun cast(): Expr {
        var expr = equality()

        if (match(AS)) {
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

        while (match(SLASH, STAR, PERCENT)) {
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

        if(checkNext(PLUS_PLUS, MINUS_MINUS)){
            val left = primary()
            val operator = advance()
            return Unary(operator, left)
        }

        return lambda()
    }

    private fun lambda(): Expr {
        val token = peek()
        if (match(FUN)) {
            val name = "<lambda ${lambdaAmount}>"
            lambdaAmount++
            currentFunction = Token.identifier(name, previous().line, previous().file)
            //while (true) {

            val typeParameters: MutableList<Param> = ArrayList()
            if (match(LESS)) {
                throw error(peek(), "Generic lambdas not supported.")
//				do {
//					if (typeParameters.size >= 255) {
//						error(peek(), "Can't have more than 255 type parameters.")
//					}
//
//					val identifier = consume(IDENTIFIER, "Expected type parameter name.")
//					typeParameters.add(Param(identifier, Type.Parameter(identifier)))
//				} while (match(COMMA))
//				consume(GREATER, "Expected '>' after type parameter declaration.")
            }

            consume(LEFT_PAREN, "Expected '(' after lambda expression")

            return finishLambda(token, name, typeParameters)
            //}
        }
        return call()
    }

    private fun call(): Expr {
        var expr = primary()

        while (true) {
            if (match(LEFT_PAREN)) {

                val typeParameters: MutableList<Param> = ArrayList()
                if (match(LESS)) {
                    //throw error(peek(), "Expected 0 type parameters.")
					var i: Int = 0
					do {
						if (typeParameters.size >= 255) {
							error(peek(), "Can't have more than 255 type parameters.")
						}

						if(sunlite.collector != null && expr is NamedExpr){
							var typeParams = sunlite.collector!!.typeHierarchy[expr.getNameToken().lexeme]?.typeParameters
                            if(typeParams == null){
                                if(expr.getExprType() is Type.Reference){
                                    typeParams = (expr.getExprType() as Type.Reference).typeParams.map { it.token.lexeme }
                                }
                            }
                            //sunlite.collector!!.findProp(expr.getNameToken(), )
							typeParameters.add(Param(Token.identifier(typeParams?.getOrNull(i) ?: "???"),getType(function = false, noColon = true)))
						} else {
							typeParameters.add(Param(Token.identifier("????"),getType(function = false, noColon = true)))
						}
						i++
					} while (match(COMMA))
					consume(GREATER, "Expected '>' after type parameter declaration.")
                }

                if (expr is GenericExpr) {
                    typeParameters.addAll(expr.getTypeArguments())
                }

                expr = finishCall(expr, typeParameters)
            } else if (match(QUESTION_DOT)) {
                val name: Token = consume(IDENTIFIER, "Expected expression after '?.'.")
                if (sunlite.collector != null) {
                    val type = sunlite.collector?.findType(
                        name,
                        Token.identifier(expr.getExprType().getName(), -1, currentFile)
                    )
                    expr = Get(expr, name, type?.getElementType() ?: Type.UNKNOWN, type?.isConstant() ?: false, true)
                } else {
                    expr = Get(expr, name, safe = true)
                }
                if(match(PLUS_PLUS, MINUS_MINUS)){
                    val operator = previous()
                    expr = Unary(operator, expr)
                }
            } else if (match(DOT)) {
                val name: Token = consume(IDENTIFIER, "Expected expression after '.'.")
                if (sunlite.collector != null) {
                    val type = sunlite.collector?.findType(
                        name,
                        Token.identifier(expr.getExprType().getName(), -1, currentFile)
                    )
                    expr = Get(expr, name, type?.getElementType() ?: Type.UNKNOWN, type?.isConstant() ?: false)
                } else {
                    expr = Get(expr, name)
                }
                if(match(PLUS_PLUS, MINUS_MINUS)){
                    val operator = previous()
                    expr = Unary(operator, expr)
                }
            } else if (match(LEFT_BRACKET)) {
                val name = expression()
                consume(RIGHT_BRACKET, "Expected ']' after expression.")
                expr = ArrayGet(expr, name, previous())
            } else {
                break
            }
        }

        return expr
    }

    private fun finishLambda(token: Token, name: String, typeParameters: MutableList<Param>): Expr {
        val parameters: MutableList<Param> = ArrayList()
        if (!checkToken(RIGHT_PAREN)) {
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

        consume(LEFT_BRACE, "Expected '{' before lambda body.")
        val body = block()

        currentFunction = null

        return Lambda(
            Stmt.Function(
                Token(
                    IDENTIFIER,
                    name,
                    null,
                    token.line,
                    currentFile,
                    token.pos
                ), FunctionType.LAMBDA, parameters, body, arrayOf(FunctionModifier.NORMAL), type, typeParameters
            )
        )
    }

    private fun finishCall(callee: Expr, typeArguments: List<Param>): Expr {
        val arguments: MutableList<Expr> = ArrayList()
        if (!checkToken(RIGHT_PAREN)) {
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
        if (match(FALSE)) return Literal(false, previous().line, previous().file, Type.BOOLEAN)
        if (match(TRUE)) return Literal(true, previous().line, previous().file, Type.BOOLEAN)
        if (match(NIL)) return Literal(null, previous().line, previous().file, Type.NIL)

        if (match(BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, STRING)) {
            when (previous().type) {
                BYTE -> return Literal(previous().literal as Byte, previous().line, previous().file, Type.BYTE)
                SHORT -> return Literal(previous().literal as Short, previous().line, previous().file, Type.SHORT)
                INT -> return Literal(previous().literal as Int, previous().line, previous().file, Type.INT)
                LONG -> return Literal(previous().literal as Long, previous().line, previous().file, Type.LONG)
                FLOAT -> return Literal(previous().literal as Float, previous().line, previous().file, Type.FLOAT)
                DOUBLE -> return Literal(previous().literal as Double, previous().line, previous().file, Type.DOUBLE)
                STRING -> return Literal(previous().literal as String, previous().line, previous().file, Type.STRING)
                else -> throw error(previous(), "Invalid literal.")
            }
        }

        if (match(SUPER)) {
            val keyword = previous()
            consume(DOT, "Expected '.' after 'super'.")
            if (!match(IDENTIFIER, INIT)) {
                throw error(peek(), "Expected superclass method name.")
            }
            val method = previous()
            if (sunlite.collector != null) {
                val type = sunlite.collector?.findType(Token.identifier("<superclass>", -1, keyword.file), currentClass)
                    ?.getElementType() ?: Type.UNKNOWN
                val methodType = sunlite.collector?.findType(
                    Token.identifier(method.lexeme, -1, keyword.file),
                    Token.identifier(type.getName(), -1, keyword.file)
                )?.getElementType() ?: Type.UNKNOWN
                return Super(keyword, method, methodType)
            } else {
                return Super(keyword, method)
            }
        }

        if (match(THIS)) {
            sunlite.collector?.let {
                val type = currentClass?.let {
                    sunlite.collector!!.findType(currentClass!!, Token.identifier("<global>", -1, currentFile))
                }

                var scope: TypeCollector.Scope? = null

                if(currentClass != null){
                    scope =
                        currentClass?.let {
                            sunlite.collector?.getValidScope(
                                sunlite.collector!!.typeScopes.first(),
                                currentClass!!,
                                Token.identifier("<global>", -1, currentFile)
                            )?.inner?.find { it.name.lexeme == currentClass?.lexeme }
                        }
                }

                val typeParams = scope?.contents?.keys?.filter { it.lexeme.startsWith("<") }

                if (typeParams?.isNotEmpty() == true) {
                    val baseGenericType = Type.ofGenericObject(
                        scope.name.lexeme,
                        typeParams.map {
                            Param(
                                Token.identifier(it.lexeme.replace("<", "").replace(">", "")),
                                Type.NULLABLE_ANY
                            )
                        })
                    return This(previous(), baseGenericType)
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

        if (match(LEFT_BRACKET)) {
            val bracket = previous()
            val list: MutableList<Expr> = ArrayList()
            if (!checkToken(RIGHT_BRACKET)) {
                do {
                    if (list.size >= 255) {
                        error(peek(), "Can't have more than 255 elements in an array literal.")
                    }
                    list.add(expression())
                } while (match(COMMA))
            }
            consume(RIGHT_BRACKET, "Expected ']' after array elements.")
            return Array(list, bracket)
        }

        if (match(IDENTIFIER)) {
            val varToken = previous()
            if (sunlite.collector != null && sunlite.compileStep > 0) {
                if(currentClass != null){
                    val scope = sunlite.collector?.typeHierarchy[currentClass!!.lexeme]?.scope
                    if(scope != null){
                        if(scope.name.lexeme == varToken.lexeme){
                            val type = scope.representing
                            return Variable(varToken, type?.getElementType() ?: Type.UNKNOWN, type?.isConstant() ?: false)
                        }
                        val pair = sunlite.collector?.findProp(
                            varToken,
                            if (currentFunction != null) currentFunction else currentClass,
                            currentBlockDepth,
                            scope
                        )
                        val type = pair?.second
                        if(pair != null && type != null){
	                        if (pair.first?.name?.lexeme != scope.name.lexeme) {
                                return Variable(varToken, type.getElementType(), type.isConstant())
	                        } else {
                                val self = This(
                                    Token(THIS, "this", null, varToken.line, varToken.file, varToken.pos),
                                    (scope.representing!!.getElementType() as Type.Reference?)?.returnType ?: Type.UNKNOWN
                                )
                                return Get(self, varToken, type.getElementType(), type.isConstant())
                            }
                        }
                    }
                }
                val type = sunlite.collector?.findType(varToken,
                    if (currentFunction != null) currentFunction else Token.identifier("<global>", -1, currentFile),
                    currentBlockDepth
                )
                return Variable(varToken, type?.getElementType() ?: Type.UNKNOWN, type?.isConstant() ?: false)
            }
            return Variable(varToken)
        }

        throw error(peek(), "Expected expression.")
    }

    private fun consume(type: TokenType, message: String): Token {
        if (checkToken(type)) return advance()

        throw error(peek(), message)
    }

    private fun assert(type: TokenType, message: String): Token {
        if (!checkToken(type)) return peek()

        throw error(peek(), message)
    }

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (checkToken(type)) {
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

    private fun checkTokens(vararg types: TokenType): Boolean {
        for (type in types) {
            if (checkToken(type)) {
                return true
            }
        }

        return false
    }

    private fun checkToken(type: TokenType): Boolean {
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
        if (current + 1 >= tokens.size) throw error(peek(), "Unexpected end of file.")
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
                CLASS, INTERFACE, FUN, VAR, FOR, IF, WHILE, RETURN, BREAK, CONTINUE -> return
                else -> {}
            }

            advance()
        }
    }


}