package sunsetsatellite.lang.sunlite

import sunsetsatellite.vm.sunlite.*
import sunsetsatellite.vm.sunlite.SLFunction
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess
import kotlin.text.split


class Sunlite(val args: Array<String>) {

	var hadError: Boolean = false
	var hadRuntimeError: Boolean = false

	val path: MutableList<String> = mutableListOf()
	val imports: MutableMap<String,List<Stmt>> = mutableMapOf()

	val logEntryReceivers: MutableList<LogEntryReceiver> = mutableListOf()
	val breakpointListeners: MutableList<BreakpointListener> = mutableListOf()

	var breakpoints: MutableMap<String,IntArray> = mutableMapOf()

	var uninitialized: Boolean = true
	lateinit var vm: VM
	var collector: TypeCollector? = null

	fun start() {
		when {
			args.size > 3 -> {
				println("Usage: sunlite [script] (path) (options)")
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
						"trace" -> bytecodeDebug = true
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

		val scanner = Scanner(data, this)
		val tokens: List<Token> = scanner.scanTokens(shortPath)

		var parser = Parser(tokens,this)
		var statements = parser.parse(shortPath)

		// Stop if there was a syntax error.
		if (hadError) return null

		collector = TypeCollector(this)
		collector?.collect(statements, shortPath)

		// Stop if there was a type collection error.
		if (hadError) return null

		parser = Parser(tokens,this)
		statements = parser.parse(shortPath)

		// Stop if there was a syntax error.
		if (hadError) return null

		val checker = TypeChecker(this, null)
		checker.check(statements)

		// Stop if there was a type error.
		if (hadError) return null

		return tokens to statements
	}

	fun compile(statements: List<Stmt>): SLFunction {
		val filePath = args[0]
		path.addAll(args[1].split(";"))
		val shortPath = filePath.split("/").last()

		val compiler = Compiler(this, vm, null)
		return compiler.compile(FunctionType.FUNCTION, Type.NIL, listOf(), statements, shortPath)
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
			runString("print($line);", null)
			hadError = false
		}
	}

	private fun runString(source: String, path: String?) {
		runVM(source, path)
		return
	}

	private fun runVM(source: String, path: String?) {
		val shortPath = path?.split("/")?.last()

		val scanner = Scanner(source, this)
		val tokens: List<Token> = scanner.scanTokens(shortPath)

		var parser = Parser(tokens,this)
		var statements = parser.parse(shortPath)

		// Stop if there was a syntax error.
		if (hadError) return

		vm = VM(this)
		uninitialized = false

		collector = TypeCollector(this)
		collector?.collect(statements, shortPath)

		// Stop if there was a type collection error.
		if (hadError) return

		if(debug) {
			printInfo("Types: ")
			printInfo("--------")
			collector?.typeScopes?.forEach { printTypeScopes(it, 0) }
			printInfo("--------")
		}

		// Stop if there was a type collection error.
		if (hadError) return

		parser = Parser(tokens,this)
		statements = parser.parse(shortPath)

		if(debug) {
			printInfo("AST: ")
			printInfo("-----")
			statements.forEach {
				printInfo(AstPrinter.print(it))
			}
			printInfo("-----")
			printInfo()
		}

		// Stop if there was a syntax error.
		if (hadError) return

		val checker = TypeChecker(this, vm)
		checker.check(statements)

		// Stop if there was a type error.
		if (hadError) return

		val compiler = Compiler(this, vm, null)

		val program: SLFunction = compiler.compile(FunctionType.FUNCTION, Type.NIL, listOf(), statements, shortPath)

		// Stop if there was a compilation error.
		if (hadError) return

		vm.call(SLClosureObj(SLClosure(program)),0)

		try {
			vm.run()
		} catch (e: Exception) {
			vm.runtimeError("internal vm error: $e")
			e.printStackTrace()
		}

	}

	fun printTypeScopes(it: TypeCollector.Scope?,depth: Int = 0){
		if(it == null) return
		val sb = StringBuilder()
		sb.append("\t".repeat(depth))
		sb.append("${it.name.lexeme} {")
		it.contents.forEach {
			sb.append("\n")
			sb.append("\t".repeat(depth+1))
			sb.append("${it.key.lexeme}${it.value}")
		}
		printInfo(sb.toString())
		it.inner.forEach { printTypeScopes(it,depth+1) }
		sb.clear()
		sb.append("\t".repeat(depth))
		sb.append("}")
		printInfo(sb.toString())
	}


	fun error(line: Int, message: String, file: String? = null) {
		reportError(line, "", message, file ?: "<unknown file>")
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
			Exception("sunlite error internal stack trace").printStackTrace()
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
			Exception("sunlite warn internal stack trace").printStackTrace()
		}
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
		var bytecodeDebug: Boolean = false

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
			val sunlite = Sunlite(args)
			logToStdout = true
			sunlite.start()
		}
	}
}