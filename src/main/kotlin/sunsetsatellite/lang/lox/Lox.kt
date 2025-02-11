package sunsetsatellite.lang.lox

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess


class Lox(val args: Array<String>) {

	var hadError: Boolean = false

	var hadRuntimeError: Boolean = false

	var interpreter: Interpreter? = null

	val typeCollector: TypeCollector = TypeCollector(this)

	val typeChecker: TypeChecker = TypeChecker(typeCollector,this)

	val path: MutableList<String> = mutableListOf()

	val imports: MutableMap<String,List<Stmt>> = mutableMapOf()

	val natives: MutableMap<String,NativeFunction<*>> = mutableMapOf()

	val globals: MutableMap<String,LoxCallable> = mutableMapOf()

	val logEntryReceivers: MutableList<LogEntryReceiver> = mutableListOf()
	val breakpointListeners: MutableList<BreakpointListener> = mutableListOf()

	var breakpoints: MutableMap<String,IntArray> = mutableMapOf()

	fun start() {
		when {
			args.size > 3 -> {
				println("Usage: lox [script] (path) (options)")
				exitProcess(64)
			}
			args.size == 1 -> {
				runFile(args[0])
			}
			args.size == 2 -> {
				path.addAll(args[1].split(";"))
				runFile(args[0])
			}
			args.size == 3 -> {
				args[2].split(";").forEach {
					when (it) {
						"debug" -> debug = true
						"stacktrace" -> stacktrace = true
						"warnStacktrace" -> warnStacktrace = true
						"stdout" -> logToStdout = true
					}
				}
				path.addAll(args[1].split(";"))
				runFile(args[0])
			}
			else -> {
				runPrompt()
			}
		}
	}

	fun parse(code: String? = null): Pair<List<Token>,List<Stmt>>? {
		val filePath = args[0]
		path.addAll(args[1].split(";"))

		val data: String = code ?: String(Files.readAllBytes(Paths.get(filePath)), Charset.defaultCharset())

		val shortPath = filePath.split("/").last()

		/*if(natives.isEmpty()){
            natives.putAll(NativeList.registerNatives())
        }*/

		val env = Environment(null, 0, "<global env>", shortPath)
		Globals.registerGlobals(env)
		if(globals.isNotEmpty()){
			globals.forEach { (k, v) -> env.define(k, v) }
		}
		interpreter = Interpreter(shortPath,env,this)

		val scanner = Scanner(data, this)
		val tokens: List<Token> = scanner.scanTokens(shortPath)

		val parser = Parser(tokens,this)
		val statements = parser.parse(shortPath)

		// Stop if there was a syntax error.
		if (hadError) return null

		val resolver = Resolver(interpreter!!,this)
		resolver.resolve(statements, shortPath)

		// Stop if there was a resolution error.
		if (hadError) return null

		typeCollector.collect(statements, shortPath)

		// Stop if there was a collection error.
		if (hadError) return null

		typeChecker.check(statements, shortPath)

		// Stop if there was a type error.
		if (hadError) return null

		filePath.let { imports[it] = statements }

		return tokens to statements
	}

	@Throws(IOException::class)
	private fun runFile(path: String) {
		val bytes = Files.readAllBytes(Paths.get(path))
		runString(String(bytes, Charset.defaultCharset()), path)

		// Indicate an error in the exit code.
		if (hadError) return //exitProcess(65)
		if (hadRuntimeError) return //exitProcess(70)
	}

	@Throws(IOException::class)
	private fun runPrompt() {
		script = true
		val input = InputStreamReader(System.`in`)
		val reader = BufferedReader(input)

		while (true) {
			print("> ")
			val line = reader.readLine() ?: break
			runString(line, null)
			hadError = false
		}
	}

	private fun runString(source: String, path: String?) {
		val shortPath = path?.split("/")?.last()

		/*if(natives.isEmpty()){
            natives.putAll(NativeList.registerNatives())
        }*/

		if(debug) {
			printInfo("Load Path: ")
			printInfo("--------")
			this.path.forEach { printInfo(it) }
			printInfo("--------")
			printInfo()
		}

		val env = Environment(null, 0, "<global env>", shortPath)
		Globals.registerGlobals(env)
		if(globals.isNotEmpty()){
			globals.forEach { (k, v) -> env.define(k, v) }
		}
		interpreter = Interpreter(shortPath,env,this)

		val scanner = Scanner(source, this)
		val tokens: List<Token> = scanner.scanTokens(shortPath)

		if(debug){
			printInfo("Tokens: ")
			printInfo("--------")
			val builder: StringBuilder = StringBuilder()
			tokens.forEachIndexed { i, it ->
				builder.append("($it)")
				if(tokens.size-1 > i) builder.append(", ")
				if(i != 0 && i % 10 == 0) builder.append("\n")
			}
			printInfo(builder.toString())
			printInfo("--------")
			printInfo()
		}


		val parser = Parser(tokens,this)
		val statements = parser.parse(shortPath)

		// Stop if there was a syntax error.
		if (hadError) return

		val resolver = Resolver(interpreter!!,this)
		resolver.resolve(statements, shortPath)

		// Stop if there was a resolution error.
		if (hadError) return

		typeCollector.collect(statements, shortPath)

		if(debug){
			printInfo("Types: ")
			printInfo("--------")
			typeCollector.info.forEach { printInfo(it) }
			printInfo("--------")
			printInfo()
		}

		if(debug) {
			printInfo("Type hierarchy: ")
			printInfo("--------")
			typeCollector.typeHierarchy.forEach { printInfo(it) }
			printInfo("--------")
			printInfo()
		}

		// Stop if there was a collection error.
		if (hadError) return

		typeChecker.check(statements, shortPath)

		// Stop if there was a type error.
		if (hadError) return

		if(debug) {
			printInfo("AST: ")
			printInfo("-----")
			statements.forEach {
				printInfo(AstPrinter.print(it))
			}
			printInfo("-----")
			printInfo()
		}


		try {
			// Evaluate a single expression
			if (statements.size == 1 && statements[0] is Stmt.Expression) {
				val evaluated = interpreter!!.evaluate((statements[0] as Stmt.Expression).expr)
				printInfo(interpreter!!.stringify(evaluated))
				return
			}
		} catch (error: LoxRuntimeError) {
			runtimeError(error)
			return
		}

		path?.let { imports[it] = statements }

		interpreter!!.interpret(statements, shortPath)
	}

	fun error(line: Int, message: String) {
		reportError(line, "", message, null)
	}

	fun error(token: Token, message: String) {
		if (token.type == TokenType.EOF) {
			reportError(token.line, " at end", message, token.file)
		} else {
			reportError(token.line, " at '" + token.lexeme + "'", message, token.file)
		}
	}

	fun warn(token: Token, message: String) {
		if (token.type == TokenType.EOF) {
			reportWarn(token.line, " at end", message, token.file)
		} else {
			reportWarn(token.line, " at '" + token.lexeme + "'", message, token.file)
		}
	}


	private fun reportError(
		line: Int, where: String,
		message: String, file: String?
	) {
		val s = "[$file, line $line] Error$where: $message"
		printErr(s)

		if(stacktrace) {
			Exception("lox error internal stack trace").printStackTrace()
		}

		hadError = true
	}

	private fun reportWarn(
		line: Int, where: String,
		message: String, file: String?
	) {
		val s = "[$file, line $line] Warn$where: $message"
		printWarn(s)

		if(warnStacktrace) {
			Exception("lox warn internal stack trace").printStackTrace()
		}
	}

	fun runtimeError(error: LoxRuntimeError) {
		if (error.token.type == TokenType.EOF) {
			val s = "[${error.token.file}, line ${error.token.line}] Error at end: ${error.message}"
			printErr(s)
		} else {
			val s = "[${error.token.file}, line ${error.token.line}] Error at '${error.token.lexeme}': ${error.message}"
			printErr(s)
		}


		if(stacktrace) {
			error.printStackTrace()
		}

		hadRuntimeError = true
	}

	fun printInfo(message: Any? = "") {
		if(logToStdout) println(message)
		logEntryReceivers.forEach { it.info(message.toString()) }
	}
	fun printWarn(message: Any? = "") {
		if(logToStdout) System.err.println(message)
		logEntryReceivers.forEach { it.warn(message.toString()) }
	}
	fun printErr(message: Any? = "") {
		if(logToStdout) System.err.println(message)
		logEntryReceivers.forEach { it.err(message.toString()) }
	}

	companion object {

		@JvmStatic
		var debug: Boolean = false

		@JvmStatic
		var stacktrace: Boolean = false

		@JvmStatic
		var warnStacktrace: Boolean = false

		@JvmStatic
		var script: Boolean = false

		@JvmStatic
		var logToStdout = false

		@JvmStatic
		@Throws(IOException::class)
		fun main(args: Array<String>) {
			val lox = Lox(args)
			logToStdout = true
			lox.start()
		}
	}
}