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
    val importingPath: String = "",
    //val subparser: Boolean = false
) {
    private var current = 0

    var currentModule: Stmt.Module? = null
    var currentFile: String? = null
    var currentClass: Token? = null
    var currentFunction: Token? = null
    var currentBlockDepth: Int = 0
    var lambdaAmount = 0
    var parsingConstructor: Boolean = false
    val importAliases: MutableMap<Token, Token> = mutableMapOf()

    val annotations: MutableList<Stmt.Annotation> = mutableListOf()

    private class ParseError : RuntimeException()

    fun start(path: String?): List<Stmt> {
        currentFile = path

        if(sunlite.autoImported.keys.none { it == path }){
            sunlite.autoImported.forEach { (path, aliases) ->
                doImport(
	                location = path,
	                what = Token.identifier("*"),
	                keyword = Token.unknown(),
	                aliases = aliases.map { Token.identifier(it) }
                )
            }
        }

        return parse()
    }

    fun parse(): List<Stmt> {
        val statements: MutableList<Stmt> = ArrayList()
        while (!isAtEnd()) {
            module()?.let {
                if(it is Stmt.VirtualStmt){
                    statements.addAll(it.decompose())
                } else {
                    statements.add(it)
                }
            }
        }

        return statements
    }

    private fun module(): Stmt? {
        try {
            return when {
                match(MODULE) -> moduleDeclaration()
                //match(module) -> packageDeclaration()
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

    private fun allowedToParse(): Boolean {
        if(importing.isEmpty()) return true
        if(importing == "*" && importingPath == currentModule?.path?.lexeme) return true
        if(importing == "*" && importingPath == currentFile) return true
        if("$importingPath::$importing" == currentModule?.path?.lexeme) return true
        return false
    }

    private fun declaration(): Stmt? {
        return when {
            match(INCLUDE) -> includeStatement()
            match(IMPORT) -> importStatement()
            match(USE) -> useStatement()
            match(VAR) -> {
                if(match(LEFT_PAREN)){
                    val decl = destructDeclaration()
                    if(allowedToParse()) decl else null
                } else {
                    val decl = varDeclaration()
                    if(allowedToParse()) decl else null
                }
            }
            match(VAL) -> {
                if(match(LEFT_PAREN)){
                    val decl = destructDeclaration(FieldModifier.CONST)
                    if(allowedToParse()) decl else null
                } else {
                    val decl = varDeclaration(FieldModifier.CONST)
                    if (allowedToParse()) decl else null
                }
            }
            match(STATIC) -> {
                val modifier = previous()
                consume(FUN, "Expected 'func' after 'static' modifier.")
                val decl = function(FunctionType.FUNCTION, modifier)
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
            match(ENUM) -> enumDeclaration()
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
            match(SUPER) -> superInitStatement()
            else -> expressionStatement()
        }
    }

    private fun moduleDeclaration(): Stmt? {
        val keyword = previous()
        var path = consume(IDENTIFIER, "Expected module name.")
        when {
            match(SEMICOLON) -> {
                if(currentModule != null){
                    throw error(previous(), "Cannot have multiple top-level module statements.")
                }
                val module = Stmt.Module(keyword, path, listOf())
                currentModule = module
                //System.err.println(AstPrinter.print(module))
                val statements: List<Stmt> = parse()
                module.stmts = statements
                if(!allowedToParse()) return null
                return module
            }
            match(LEFT_BRACE) -> {
                val previous = currentModule
                if(currentModule != null){
                    path = Token.identifier("${currentModule!!.path.lexeme}::${path.lexeme}", path)
                }
                currentBlockDepth++
                val statements: MutableList<Stmt> = ArrayList()
                val module = Stmt.Module(keyword, path, statements)
                currentModule = module
                while (!checkToken(RIGHT_BRACE) && !isAtEnd()) {
                    module()?.let { statements.add(it) } ?: break
                }

                consume(RIGHT_BRACE, "Expected '}' after module block.")
                currentBlockDepth--
                module.stmts = statements
                //System.err.println(AstPrinter.print(module))
                currentModule = previous
                if(!allowedToParse()) return null
                return module
            }
        }
        return null
    }

    private fun useStatement(): Stmt? {
        val keyword = previous()
        val path = consume(IDENTIFIER, "Expected fully qualified type name for use statement.")
        consume(AS, "Expected 'as' after type name in use statement.")
        val alias = consume(IDENTIFIER, "Expected identifier for use alias.")
        consume(SEMICOLON, "Expected ';' after use statement.")
        addAlias(alias, path)
        return null
    }

    private fun importStatement(): Stmt? {
        val keyword = previous()
        val aliases: MutableList<Token> = mutableListOf()
        when {
            match(LEFT_PAREN) -> {
                do {
                    aliases.add(consume(IDENTIFIER, "Expected identifier for import alias."))
                } while (match(COMMA))
                consume(RIGHT_PAREN, "Expected ')' after import aliases.")
            }
            match(IDENTIFIER) -> {
                aliases.add(previous())
            }
        }
        if(aliases.isNotEmpty()) consume(FROM, "Expected 'from' after import aliases.")
        val path = consume(IDENTIFIER, "Expected module name for import statement.").lexeme
        consume(SEMICOLON, "Expected ';' after import statement.")
        val last = "*"
        return doImport(path, Token.identifier(last,keyword), keyword, aliases)
    }

    private fun doImport(
        location: String,
        what: Token,
        keyword: Token,
        aliases: List<Token>
    ): Stmt.Import? {
        importAliases.putAll(aliases.map { it to Token.identifier(location+"::"+it.lexeme,it) })

        //if(sunlite.compileStep >= Sunlite.MAX_COMPILE_STEP) System.err.println("$location.${what.lexeme}")
        val id = location //+ "::" + what.lexeme
        if (sunlite.imports.contains(id)) {
            return null //Stmt.Import(keyword, what, location, aliases)
        }
        sunlite.imports[id] = includingDepth to null
        if (sunlite.collector == null || !allowIncluding) {
            return null //Stmt.Import(keyword, what, location, aliases)
        }

        var data: String? = null
        val invalidPaths: MutableList<String> = mutableListOf()
        val path = sunlite.modulePathReadFunction.apply(location)

        data = Sunlite::class.java.getResourceAsStream(path)?.bufferedReader()?.use { it.readText() }

        if (data == null) {
            sunlite.path.forEach {
                try {
                    data = sunlite.readFunction.apply("${it.replace("\\","/")}${path}.sl")
                } catch (_: IOException) {
                    invalidPaths.add(it)
                }
            }
        }

        if (data == null) {
            sunlite.imports.remove(id)
            sunlite.error(keyword, "ImportError: Can't find module '${location}'.")
            return null
        }

        val scanner = Scanner(data, sunlite)
        val tokens: List<Token> = scanner.scanTokens(location)

        var parser = Parser(tokens, sunlite, true, true, includingDepth + 1, what.lexeme, location)
        var statements = parser.start(location)

        // Stop if there was a syntax error.
        if (sunlite.hadError) {
            sunlite.imports.remove(id)
            //sunlite.error(keyword, "ImportError: SyntaxError in file being imported.")
            return null
        }

        sunlite.collector?.collect(statements, location, sunlite.compileStep)

        // Stop if there was a type collection error.
        if (sunlite.hadError) {
            sunlite.imports.remove(id)
            //sunlite.error(keyword, "ImportError: TypeError in file being imported.")
            return null
        }

        parser = Parser(tokens, sunlite, true, true, includingDepth + 1, what.lexeme, location)
        statements = parser.start(location)

        // Stop if there was a syntax error.
        if (sunlite.hadError) {
            sunlite.imports.remove(id)
            //sunlite.error(keyword, "ImportError: SyntaxError in file being imported.")
            return null
        }

        sunlite.collector?.collect(statements, location, sunlite.compileStep + 2)

        if(sunlite.compileStep > 0){
            val checker = TypeChecker(sunlite, null)
            checker.check(statements)
        }

        // Stop if there was a type error.
        if (sunlite.hadError) {
            sunlite.imports.remove(id)
            //sunlite.error(keyword, "ImportError: TypeError in file being imported.")
            return null
        }

        sunlite.imports[id] = includingDepth to statements

        if (Sunlite.showOtherAST && sunlite.compileStep >= Sunlite.MAX_COMPILE_STEP) {
            sunlite.printDebug("AST: ${location}")
            sunlite.printDebug("-----")
            statements.forEach {
                sunlite.printDebug(AstPrinter.print(it))
            }
            sunlite.printDebug("-----")
            sunlite.printDebug()
        }

        if (Sunlite.debug && sunlite.compileStep >= Sunlite.MAX_COMPILE_STEP) {
            sunlite.printDebug("Imported ${what.lexeme} from ${location}.")
            //sunlite.printInfo()
        }
        /*if(currentModule != null){
            currentModule!!.importAliases.addAll(importAliases)
        }*/
        return null//Stmt.Import(keyword, what, location, aliases)
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
                    data = sunlite.readFunction.apply(it + what.literal + ".sl`")
                } catch (_: IOException) {
                    invalidPaths.add(it)
                }
            }
        } else {
            data = Sunlite::class.java.getResourceAsStream(what.literal + ".sl")?.bufferedReader()?.use { it.readText() }
        }


        if (data == null) {
            sunlite.error(keyword, "ImportError: Couldn't find '${what.literal}' on the load path list.")
            return null
        }

        val scanner = Scanner(data, sunlite)
        val tokens: List<Token> = scanner.scanTokens(what.literal)

        var parser = Parser(tokens, sunlite, true, true, includingDepth + 1)
        var statements = parser.start(what.literal)

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
        statements = parser.start(what.literal)

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

        /*if (Sunlite.showAST) {
            sunlite.printInfo("AST: ${currentFile}")
            sunlite.printInfo("-----")
            statements.forEach {
                sunlite.printInfo(AstPrinter.print(it))
            }
            sunlite.printInfo("-----")
            sunlite.printInfo()
        }*/

        if (Sunlite.debug) {
            sunlite.printDebug("Parsed and included '${what.literal}'.")
        }

        return Stmt.Include(keyword, what)
    }

    private fun classDeclaration(modifier: ClassModifier): Stmt? {
        //if(modifier == ClassModifier.DYNAMIC) consume(CLASS, "Expected 'class' after class modifier.")

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

        var name = consume(IDENTIFIER, "Expected class name.")
        val rawName = name
        currentModule?.let { module -> name = Token.identifier("${module.path.lexeme}::${name.lexeme}",name) }
        currentClass = name
        addAlias(rawName, name)

        var superclass: Variable? = null
        if (match(EXTENDS)) {
            val token = resolveAlias(consume(IDENTIFIER, "Expected superclass name."))
            superclass = Variable(token)
        }
        if(superclass == null && name.lexeme != "sunlite::stdlib::object::Object"){
            superclass = Variable(Token.identifier("sunlite::stdlib::object::Object", previous()))
        }

        val superinterfaces: MutableList<Pair<Variable,List<Type>>> = mutableListOf()
        if (match(IMPLEMENTS)) {
            do {
                if (superinterfaces.size >= 255) {
                    error(peek(), "Can't inherit more than 255 superinterfaces.")
                }

                val nameToken = resolveAlias(consume(IDENTIFIER, "Expected superinterface name."))
                val name = Variable(nameToken)
                val specifiedTypeParams: MutableList<Type> = mutableListOf()
                if (match(LESS)) {
                    do {
                        if (typeParameters.size >= 255) {
                            error(peek(), "Can't have more than 255 type parameters.")
                        }
                        val type = getType(false, true)
                        specifiedTypeParams.add(type)
                    } while (match(COMMA))
                    consume(GREATER, "Expected '>' after type parameter declaration.")
                }

                superinterfaces.add(name to specifiedTypeParams)

            } while (match(COMMA))
        }

        consume(LEFT_BRACE, "Expected '{' before class body.")

        sunlite.collector?.let { collector ->
            for (superinterface in superinterfaces) {
                val prototype = collector.typeHierarchy[superinterface.first.name.lexeme]

                prototype?.let { p ->
                    typeParameters.addAll(p.typeParameters.mapIndexed { index, it ->
                        val identifier = Token.identifier(it, superinterface.first)
                        Param(identifier, superinterface.second.getOrElse(index, { Type.Parameter(identifier) }))
                    })
                }
            }
        }

        if(allowedToParse()){
            val types = sunlite.collector?.typeHierarchy
            if(types?.containsKey(name.lexeme) == false || (types?.containsKey(name.lexeme) == true && types[name.lexeme]?.incomplete == true)){
	            types[name.lexeme] = TypeCollector.TypePrototype(
                    name.lexeme,
                    superclass?.name?.lexeme ?: "<nil>",
                    superinterfaces.map { it.first.name.lexeme },
                    typeParameters.map { it.token.lexeme },
                    modifier,
                    null,
		            incomplete = true,
		            isInterface = false,
		            id = sunlite.compileStep
                )
            }
        }


        val methods: MutableList<Stmt.Function> = ArrayList()
        val fields: MutableList<Stmt.Var> = ArrayList()
        var staticInit: Stmt.Block? = null
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

                            match(INIT) -> {
                                consume(LEFT_BRACE, "Expected '{' before static initializer body.")
                                staticInit = Stmt.Block(block(), previous().line, previous().file)
                            }

                            else -> {
                                throw error(peek(), "Expected a field or method declaration.")
                            }
                        }
                    } else if (match(FUN)) {
                        //throw error(peek(), "Expected 'static' before native method declaration.")
                        methods.add(function(FunctionType.METHOD, currentModifier))
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

                match(AT) -> {
                    annotationDeclaration()
                }

                else -> {
                    throw error(peek(), "Expected a field or method declaration.")
                }
            }
        }

        consume(RIGHT_BRACE, "Expected '}' after class body.")

        currentClass = null
        importAliases.remove(rawName)

        if(!allowedToParse()) return null

        return Stmt.Class(name, methods, fields, superclass, superinterfaces.map { it.first }, modifier, typeParameters, staticInit)
    }

    private fun enumDeclaration(): Stmt? {
        var name = consume(IDENTIFIER, "Expected enum name.")
        val rawName = name
        currentModule?.let { module -> name = Token.identifier("${module.path.lexeme}::${name.lexeme}",name) }
        currentClass = name
        addAlias(rawName, name)

        val superclass = Variable(Token.identifier("Enum", previous()))

        consume(LEFT_BRACE, "Expected '{' before enum body.")

        if(allowedToParse()){
            val types = sunlite.collector?.typeHierarchy
            if(types?.containsKey(name.lexeme) == false || (types?.containsKey(name.lexeme) == true && types[name.lexeme]?.incomplete == true)){
                types[name.lexeme] = TypeCollector.TypePrototype(
                    name.lexeme,
	                superclass.name.lexeme,
                    listOf(),
                    listOf(),
                    ClassModifier.SEALED,
                    null,
	                incomplete = true,
	                isInterface = false,
	                id = sunlite.compileStep
                )
            }
        }

        val methods: MutableList<Stmt.Function> = ArrayList()
        val fields: MutableList<Stmt.Var> = ArrayList()
        var staticInit: Stmt.Block? = null

        if(checkToken(IDENTIFIER)){
            do {
                val valueName = peek()
                advance()
                val enumType = Type.ofObject(name.lexeme)
                val enumClass = Variable(name, enumType, true)
	            val initializer: Call =
                    if(checkToken(LEFT_PAREN)){
                        advance()
                        finishCall(enumClass, listOf()) as Call
                    } else {
                        Call(enumClass, valueName, listOf(), listOf())
                    }
                fields.add(Stmt.Var(valueName, enumType, initializer, FieldModifier.STATIC_CONST))
            } while (match(COMMA))
            consume(SEMICOLON, "Expected ';' after enum values declaration.")
        }

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

                            match(INIT) -> {
                                consume(LEFT_BRACE, "Expected '{' before static initializer body.")
                                staticInit = Stmt.Block(block(), previous().line, previous().file)
                            }

                            else -> {
                                throw error(peek(), "Expected a field or method declaration.")
                            }
                        }
                    } else if (match(FUN)) {
                        //throw error(peek(), "Expected 'static' before native method declaration.")
                        methods.add(function(FunctionType.METHOD, currentModifier))
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

        consume(RIGHT_BRACE, "Expected '}' after enum body.")

        currentClass = null
        importAliases.remove(rawName)

        if(!allowedToParse()) return null

        return Stmt.Class(name, methods, fields, superclass, listOf(), ClassModifier.SEALED, listOf(), staticInit)
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

        var name = consume(IDENTIFIER, "Expected interface name.")
        val rawName = name
        currentModule?.let { module -> name = Token.identifier("${module.path.lexeme}::${name.lexeme}",name) }
        addAlias(rawName, name)

        val superinterfaces: MutableList<Variable> = mutableListOf()
        if (match(IMPLEMENTS)) {
            do {
                if (superinterfaces.size >= 255) {
                    error(peek(), "Can't inherit more than 255 superinterfaces.")
                }

                superinterfaces.add(
                    Variable(resolveAlias(consume(IDENTIFIER, "Expected superinterface name.")))
                )
            } while (match(COMMA))
        }

        consume(LEFT_BRACE, "Expected '{' before interface body.")

        if(allowedToParse()){
            val types = sunlite.collector?.typeHierarchy
            if(types?.containsKey(name.lexeme) == false || (types?.containsKey(name.lexeme) == true && types[name.lexeme]?.incomplete == true)){
                types[name.lexeme] = TypeCollector.TypePrototype(
                    name.lexeme,
                    "<nil>",
                    superinterfaces.map { it.name.lexeme },
                    typeParameters.map { it.token.lexeme },
                    ClassModifier.ABSTRACT,
                    null,
	                incomplete = true,
	                isInterface = false,
	                id = sunlite.compileStep
                )
            }
        }

        val methods: MutableList<Stmt.Function> = ArrayList()
        while (!checkToken(RIGHT_BRACE) && !isAtEnd()) {
            methods.add(abstractMethod())
        }

        consume(RIGHT_BRACE, "Expected '}' after interface body.")

        importAliases.remove(rawName)
        if(!allowedToParse()) return null

        return Stmt.Interface(name, methods, superinterfaces, typeParameters)
    }

    private fun superInitStatement(): Stmt {
        current--
        val expr = call()
        consume(SEMICOLON, "Expected ';' after super constructor call.")
        if(expr is Call){
            val desc = "init"+Type.ofFunction("init", Type.NIL, expr.arguments.map { Param((it as NamedExpr).getNameToken(), it.getExprType()) }).getDescriptor()
            (expr.callee as Super).method = Token.identifier(desc, expr.callee.getNameToken())
            return Stmt.SuperInit(expr)
        }
        throw error(peek(), "Expected '(' after 'super' in super constructor call.")
    }

    data class FuncSignature(val name: Token, val parameters: List<Param>, val type: Type, val receiver: Token? = null)

    private fun funcSignature(kind: FunctionType): FuncSignature {
        var name = if (kind == FunctionType.INITIALIZER) Token.identifier("init", -1, currentFile) else consume(
            IDENTIFIER,
            "Expected ${kind.toString().lowercase()} name."
        )

        var receiver: Token? = null
        if(match(DOT)){
            if(kind != FunctionType.FUNCTION){
                throw error(peek(), "Only top level functions can be extensions.")
            }
            receiver = name
            sunlite.collector!!.typeHierarchy[receiver.lexeme]?.let {
	            if (it.isInterface) {
		            throw error(peek(), "Can't add extensions to interfaces.")
	            }
            }
            name = consume(IDENTIFIER, "Expected ${kind.toString().lowercase()} name after extension receiver.")

        }

        consume(LEFT_PAREN, "Expected '(' after ${kind.toString().lowercase()} name.")
        val parameters: MutableList<Param> = ArrayList()
        if (!checkToken(RIGHT_PAREN)) {
            do {
                if (parameters.size >= 255) {
                    error(peek(), "Can't have more than 255 parameters.")
                }

                parameters.add(
                    Param(consume(IDENTIFIER, "Expected parameter name."), getType()/*, continueIf(EQUAL){ expression() }*/)
                )
            } while (match(COMMA))
        }
        consume(RIGHT_PAREN, "Expected ')' after parameters.")

        val type = getType(function = true)

        if (kind == FunctionType.INITIALIZER) {
            val desc = "init"+Type.ofFunction("init", Type.NIL, parameters).getDescriptor()
            name = Token.identifier(desc, -1, currentFile)
            parsingConstructor = true
        }

        currentFunction = name
        return FuncSignature(name, parameters, type, receiver)
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
        parsingConstructor = false

        return Stmt.Function(
            signature.name,
            FunctionType.METHOD,
            signature.parameters,
            listOf(),
            arrayOf(FunctionModifier.ABSTRACT),
            signature.type,
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

        if(!funcModifier.contains(FunctionModifier.NORMAL) && !funcModifier.contains(FunctionModifier.STATIC) && signature.receiver != null){
            throw error(signature.receiver, "Extension ${kind.toString().lowercase()} can only have 'static' or no modifier.")
        }

        if(kind == FunctionType.FUNCTION && funcModifier.contains(FunctionModifier.STATIC) && signature.receiver == null){
            throw error(peek(), "Only extension ${kind.toString().lowercase()}s can have the 'static' modifier at top level.")
        }

        var body: List<Stmt> = listOf()
        if (!funcModifier.contains(FunctionModifier.NATIVE)) {
            consume(LEFT_BRACE, "Expected '{' before ${kind.toString().lowercase()} body.")
            body = block()
        } else {
            assert(LEFT_BRACE, "Native ${kind.toString().lowercase()} cannot have a body.")
        }

        currentFunction = null
        parsingConstructor = false

        val ann = ArrayList<Stmt.Annotation>(annotations)
        annotations.clear()

        return Stmt.Function(
            signature.name,
            kind,
            signature.parameters,
            body,
            funcModifier,
            signature.type,
            typeParameters,
            ann,
            signature.receiver
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
        var element: Stmt
        var destructing = false
        if(match(LEFT_PAREN)){
            destructing = true
            element = destructDeclaration(FieldModifier.NORMAL, true)
        } else {
            element = varDeclaration(FieldModifier.NORMAL, true)
        }
        //var element = varDeclaration(FieldModifier.NORMAL, true)
        consume(IN, "Expected 'in' after variable declaration for 'foreach' loop.")
        val collection = expression()
        consume(RIGHT_PAREN, "Expected ')' after 'foreach' clauses.")
        val iterType: Type.Reference = let {
            val prototype = sunlite.collector?.typeHierarchy[collection.getExprType().getName()] ?: return@let Type.ofObject("Iterator")
            val element = prototype.scope?.contents?.firstNotNullOfOrNull { if(it.key.lexeme == "getIterator") it.value else null } as? TypeCollector.FunctionPrototype ?: return@let Type.ofObject("Iterator")
            val rawType = element.returnType
            if (collection.getExprType() !is Type.Reference) {
                return@let Type.ofObject("Iterator")
            }
            val reference = collection.getExprType() as Type.Reference
            when {
                reference.type == PrimitiveType.OBJECT -> {
                    return@let Type.reify(rawType, reference.typeParams) as Type.Reference
                }
            }
            return@let Type.ofObject("Iterator")
        }
        val initCall = Call(
            Get(collection, Token.identifier("getIterator", collection), iterType),
            Token.identifier("<synthetic iterator init call>", collection),
            listOf(),
            listOf()
        )
        val initializer = Stmt.Var(
            Token.identifier("<iter>", collection),
            iterType, initCall, FieldModifier.NORMAL
        )
        val iterVar = Variable(Token.identifier("<iter>", collection), iterType)
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
        var type: Type
        val exprType = collection.getExprType()
        if(exprType is Type.Reference){
            val cType = exprType
            when {
                cType.type == PrimitiveType.ARRAY -> {
                    type = cType.returnType
                }
                cType.type == PrimitiveType.OBJECT -> {
                    if(iterType.typeParams.size >= 1){
                        type = iterType.typeParams[0].type
                    } else {
                        type = Type.UNKNOWN
                    }
                }
                else -> {
                    type = Type.UNKNOWN
                }
            }
        } else {
            type = Type.UNKNOWN
        }

        /*sunlite.collector?.let {
            if(collectionType is Type.Reference && collectionType.type == PrimitiveType.OBJECT) {
                val prototype = it.typeHierarchy[collectionType.getName()]
                prototype?.let { prototype ->
                    prototype.scope?.let { scope ->
                        val typeParam = scope.contents.mapKeys { it.key.lexeme }["<T>"]
                        typeParam?.let { typeParam ->
                            type = typeParam.getElementType()
                        }
                    }
                }
            }
        }*/

        if(!destructing) {
            val e = element as Stmt.Var
            if(e.type == Type.UNKNOWN){
                element = Stmt.Var(e.name, type, null, e.modifier)
            }
        }

        val increment = Call(
            Get(
                iterVar,
                Token.identifier("next", collection),
                Type.ofFunction("next",
                    type,
                    listOf())
            ),
            Token.identifier("<synthetic iterator next call>", collection),
            listOf(),
            listOf()
        )
        val currentCall = Call(
            Get(
                iterVar,
                Token.identifier("current", Token.unknown()),
                Type.ofFunction(
                    "current",
                    type,
                    listOf()
                )
            ),
            Token.identifier("<synthetic iterator current call>", Token.unknown()),
            listOf(),
            listOf(),
        )
        if(!destructing){
            val e = element as Stmt.Var
            element = Stmt.Var(
                e.name, e.type,
                currentCall, FieldModifier.NORMAL
            )
        } else {
            (element as Stmt.Destruct).collection = currentCall
        }

        var body = statement()
        val stmts: MutableList<Stmt> = mutableListOf()
        if(destructing){
            stmts.addAll((element as Stmt.Destruct).decompose())
        } else {
            stmts.add(element)
        }
        stmts.add(body)

        body = Stmt.Block(stmts, peek().line, peek().file)
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
        return null/*when {
            match(AT) -> annotationDeclaration()
            //match(VAR) -> varDeclaration()
            //match(VAL) -> varDeclaration(FieldModifier.CONST)
            match(FUN) -> function(FunctionType.FUNCTION, null)
            //match(CLASS) -> classDeclaration(ClassModifier.NORMAL)
            //match(INTERFACE) -> interfaceDeclaration()
            else -> throw error(previous(), "Expected annotatable declaration after annotation.")
        }*/
    }

    private fun destructDeclaration(modifier: FieldModifier = FieldModifier.NORMAL, foreach: Boolean = false): Stmt.Destruct {
        val vars: MutableList<Param> = mutableListOf()

        if (!checkToken(RIGHT_PAREN)) {
            do {
                if (vars.size >= 255) {
                    error(peek(), "Can't destruct into more than 255 variables.")
                }

                vars.add(
                    Param(consume(IDENTIFIER, "Expected variable name."), getType(foreach = true))
                )
            } while (match(COMMA))
        }
        consume(RIGHT_PAREN, "Expected ')' after variable names.")

        var collection: Expr? = null
        if(!foreach){
            consume(EQUAL, "Expected '=' after ')'.")
            collection = expression()
            consume(SEMICOLON, "Expected ';' after destructing declaration.")
        }

        return Stmt.Destruct(vars, collection, modifier)
    }

    private fun varDeclaration(modifier: FieldModifier = FieldModifier.NORMAL, foreach: Boolean = false): Stmt.Var {
        val name = consume(IDENTIFIER, "Expected variable name.")

        var type = getType(foreach = true)

        var initializer: Expr? = null
        if (match(EQUAL)) {
            if(foreach){
                throw error(peek(), "Foreach element variable cannot have an initializer.")
            }
            initializer = expression()
        }

        if(type == Type.UNKNOWN && initializer != null){
            type = initializer.getExprType()
        } else if(type == Type.UNKNOWN && initializer == null && !foreach) {
            throw error(peek(), "Can't infer type of variable without an initializer.")
        }

        if (!foreach) consume(SEMICOLON, "Expected ';' after variable declaration.")
        return Stmt.Var(name, type, initializer, modifier)
    }

    private fun getTypeTokens(insideUnion: Boolean = false): List<TypeToken> {
        var mainToken = peek()
        if (!match(
                TYPE_BOOLEAN, TYPE_STRING, TYPE_TUPLE,/*TYPE_NUMBER,*/
                TYPE_BYTE, TYPE_SHORT, TYPE_INT, TYPE_LONG, TYPE_FLOAT, TYPE_DOUBLE,
                TYPE_FUNCTION, TYPE_CLASS, TYPE_ANY, TYPE_GENERIC, TYPE_ARRAY, TYPE_TABLE, IDENTIFIER, TYPE_NIL
            )
        ) {
            throw error(mainToken, "Expected type.")
        }

        if(mainToken.type == IDENTIFIER){
            mainToken = resolveAlias(mainToken)
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
                if (!checkTypes()) {
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
                if (!checkTypes()) {
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

    private fun getType(function: Boolean = false, noColon: Boolean = false, foreach: Boolean = false): Type {
        var type: Type = if (function) Type.NIL else if(foreach) Type.UNKNOWN else Type.ANY
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
        val expr = elvis()

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

                    is Tuple -> {
                        var type: Type = Type.UNKNOWN
                        val valueType = value.getExprType()
                        if(valueType is Type.Reference){
                            if(valueType.type == PrimitiveType.TUPLE || valueType.type == PrimitiveType.ARRAY){
                                type = valueType.returnType
                            }
                        }
                        return MultiSet(expr.expr, value, previous(), type)
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

    private fun elvis(): Expr {
        val expr: Expr = orExpr()

        if(match(QUESTION_COLON)){
            val right: Expr = orExpr()
            val condition = Binary(
                expr,
                Token(BANG_EQUAL, "!=", null, peek().line, peek().file, Token.Position(-1, -1)),
                Literal(null, previous().line, previous().file, Type.NIL)
            )
            return If(condition, expr, right, getIfExprType(expr, right))
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
                var name: Expr
                val type = expr.getExprType()
                if(type is Type.Reference && type.type == PrimitiveType.TUPLE){
                    name = run {
                        return@run if (match(BYTE, SHORT, INT)) {
                            when (previous().type) {
                                BYTE -> Literal(previous().literal as Byte, previous().line, previous().file, Type.BYTE)
                                SHORT -> Literal(previous().literal as Short, previous().line, previous().file, Type.SHORT)
                                INT -> Literal(previous().literal as Int, previous().line, previous().file, Type.INT)
                                else -> throw error(previous(), "Tuple index can only be a constant integer.")
                            }
                        } else {
                            throw error(previous(), "Expected constant integer as tuple index.")
                        }
                    }
                } else {
                    name = expression()
                }
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
        parsingConstructor = false

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
            if(currentFunction != null && parsingConstructor){
                return Super(keyword, Token.identifier("<super init>", keyword), Type.NIL)
            }
            consume(DOT, "Expected '.' after 'super'.")
            if (!match(IDENTIFIER)) {
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
            val paren = previous()
            val expr = expression()
            if(checkToken(RIGHT_PAREN)) {
                consume(RIGHT_PAREN, "Expected ')' after grouping expression.")
                return Grouping(expr)
            } else if(checkToken(COMMA)){
                advance()
                val list: MutableList<Expr> = ArrayList()
                list.add(expr)
                if (!checkToken(RIGHT_PAREN)) {
                    do {
                        if (list.size >= 255) {
                            error(peek(), "Can't have more than 255 elements in an tuple literal.")
                        }
                        list.add(expression())
                    } while (match(COMMA))
                }
                consume(RIGHT_PAREN, "Expected ')' after tuple elements.")
                return Tuple(list, paren)
            }
        }

        /*if (checkTypes()) {
            val p = subparser()
            val type = p.getType(function = false, noColon = true)
            sunlite.hadError = false
            if (p.match(LEFT_BRACKET)) {
                current = p.current
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
                return Array(list, bracket, type)
            }
        }*/

        if (match(LEFT_BRACKET)) {
            val bracket = previous()
            var type: Type = Type.UNKNOWN
            if(match(LESS) && checkTypes()){
                type = getType(function = false, noColon = true)
                consume(GREATER, "Expected '>' after type parameter declaration.")
            }
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
            if(type == Type.UNKNOWN){
                type = list.first().getExprType()
            }
            return Array(list, bracket, Type.ofArray(type))
        }

        if (match(IDENTIFIER)) {
            val varToken = resolveAlias(previous())

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

        if(match(IF)){
            consume(LEFT_PAREN, "Expected '(' after 'if'.")
            val condition = expression()
            consume(RIGHT_PAREN, "Expected ')' after 'if' condition.")

            val thenBranch = expression()
            consume(ELSE, "Expected 'else' after ')' in if expression.")
            val elseBranch: Expr = expression()

            val type: Type = getIfExprType(thenBranch, elseBranch)

            return If(condition, thenBranch, elseBranch, type)
        }

        throw error(peek(), "Expected expression.")
    }

    fun getIfExprType(
        thenBranch: Expr,
        elseBranch: Expr
    ): Type = if (thenBranch.getExprType() is Type.Singular && elseBranch.getExprType() is Type.Singular) {
        if (Type.contains(thenBranch.getExprType(), elseBranch.getExprType(), null, sunlite)) {
            thenBranch.getExprType()
        } else if (Type.contains(elseBranch.getExprType(), thenBranch.getExprType(), null, sunlite)) {
            elseBranch.getExprType()
        } else {
            Type.Union(listOf(thenBranch.getExprType(), elseBranch.getExprType()) as List<Type.Singular>)
        }
    } else {
        val types: MutableList<Type.Singular> = mutableListOf()
        if (thenBranch.getExprType() is Type.Union) {
            types.addAll((thenBranch.getExprType() as Type.Union).types)
        } else {
            types.add(thenBranch.getExprType() as Type.Singular)
        }
        if (elseBranch.getExprType() is Type.Union) {
            types.addAll((thenBranch.getExprType() as Type.Union).types)
        } else {
            types.add(elseBranch.getExprType() as Type.Singular)
        }
        Type.Union(types)
    }

    private fun addAlias(alias: Token, path: Token) {
        if(Sunlite.debug && sunlite.compileStep >= Sunlite.MAX_COMPILE_STEP){
            sunlite.printDebug("Adding alias: ${path.lexeme} as ${alias.lexeme}")
        }
        importAliases[alias] = path
    }

    private fun resolveAlias(alias: Token): Token {
        importAliases.filter { it.key.lexeme == alias.lexeme }.firstNotNullOfOrNull { it.value }?.let {
            if(Sunlite.debug && sunlite.compileStep >= Sunlite.MAX_COMPILE_STEP){
                sunlite.printDebug("Resolved alias: ${alias.lexeme} -> ${it.lexeme}")
            }
            return Token.identifier(it.lexeme, alias)
        }
        return alias
    }

    /*private fun subparser(): Parser {
        val p = Parser(ArrayList(tokens), sunlite, allowIncluding, including, includingDepth, importing, true)
        p.current = current
        return p
    }*/

    private fun consume(type: TokenType, message: String): Token {
        if (checkToken(type)) return advance()

        throw error(peek(), message)
    }

	private fun <T> continueIf(type: TokenType, action: (Token) -> T): T? {
		if (checkToken(type)) return action(advance())

		return null
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

    private fun checkTypes(): Boolean {
        return checkTokens(
            TYPE_BOOLEAN, TYPE_STRING, TYPE_TUPLE,
            TYPE_BYTE, TYPE_SHORT, TYPE_INT, TYPE_LONG, TYPE_FLOAT, TYPE_DOUBLE,
            TYPE_FUNCTION, TYPE_CLASS, TYPE_ANY, TYPE_GENERIC, TYPE_ARRAY, TYPE_TABLE, IDENTIFIER, TYPE_NIL
        )
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