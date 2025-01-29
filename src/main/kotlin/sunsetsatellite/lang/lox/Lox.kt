package sunsetsatellite.lang.lox

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess


object Lox {

	@JvmStatic
	var hadError: Boolean = false
	@JvmStatic
	var hadRuntimeError: Boolean = false

	@JvmStatic
	var debug: Boolean = false

	@JvmStatic
	private val interpreter: Interpreter = Interpreter()

	@JvmStatic
	@Throws(IOException::class)
	fun main(args: Array<String>) {
		if (args.size > 1) {
			println("Usage: lox [script]");
			exitProcess(64);
		} else if (args.size == 1) {
			runFile(args[0]);
		} else {
			runPrompt();
		}
	}

	@Throws(IOException::class)
	private fun runFile(path: String) {
		val bytes = Files.readAllBytes(Paths.get(path))
		runString(String(bytes, Charset.defaultCharset()))

		// Indicate an error in the exit code.
		if (hadError) exitProcess(65)
		if (hadRuntimeError) exitProcess(70)
	}

	@Throws(IOException::class)
	private fun runPrompt() {
		val input = InputStreamReader(System.`in`)
		val reader = BufferedReader(input)

		while (true) {
			print("> ")
			val line = reader.readLine() ?: break
			runString(line)
			hadError = false;
		}
	}

	private fun runString(source: String) {
		val scanner = Scanner(source)
		val tokens: List<Token> = scanner.scanTokens()

		println("Tokens: ")
		println("--------")
		val builder: StringBuilder = StringBuilder()
		tokens.forEachIndexed { i, it ->
			builder.append(it.toString())
			if(tokens.size-1 > i) builder.append(", ")
		}
		println(builder.toString())
		println()

		val parser = Parser(tokens)
		val statements = parser.parse()

		// Stop if there was a syntax error.
		if (hadError) return

		val resolver = Resolver(interpreter)
		resolver.resolve(statements)

		// Stop if there was a resolution error.
		if (hadError) return;
		
		println("AST: ")
		println("-----")
		statements.forEach {
			println(AstPrinter.print(it))
		}
		println()

		try {
			// Evaluate a single expression
			if (statements.size == 1 && statements[0] is Stmt.Expression) {
				val evaluated = interpreter.evaluate((statements[0] as Stmt.Expression).expr)
				println(interpreter.stringify(evaluated))
				return
			}
		} catch (error: LoxRuntimeError) {
			runtimeError(error)
			return
		}

		interpreter.interpret(statements)
	}

	fun error(line: Int, message: String) {
		report(line, "", message)
	}

	fun error(token: Token, message: String) {
		if (token.type == TokenType.EOF) {
			report(token.line, " at end", message)
		} else {
			report(token.line, " at '" + token.lexeme + "'", message)
		}
	}


	private fun report(
		line: Int, where: String,
		message: String
	) {
		System.err.println(
			"[line $line] Error$where: $message"
		)

		if(debug) {
			Exception("runtime stack trace").printStackTrace()
		}

		hadError = true
	}

	fun runtimeError(error: LoxRuntimeError) {
		System.err.println(
			error.message +
					"\n[line " + error.token.line + "]"
		)

		if(debug) {
			error.printStackTrace()
		}

		hadRuntimeError = true
	}
}