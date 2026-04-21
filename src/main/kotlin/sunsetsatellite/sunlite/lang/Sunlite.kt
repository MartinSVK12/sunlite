package sunsetsatellite.sunlite.lang

import sunsetsatellite.sunlite.vm.*
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.util.function.Function
import java.util.zip.GZIPInputStream
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.system.exitProcess

class Sunlite(val args: Array<String>) {

    var hadError: Boolean = false
    var hadRuntimeError: Boolean = false
    var compileStep: Int = 0

    val path: MutableList<String> = mutableListOf()
    val includes: MutableMap<String, Pair<Int, List<Stmt>>> = mutableMapOf()

    val logEntryReceivers: MutableList<LogEntryReceiver> = mutableListOf()
    val dataReceiver: MutableList<CompilerDataReceiver> = mutableListOf()
    val breakpointListeners: MutableList<BreakpointListener> = mutableListOf()

    var breakpoints: MutableMap<String, IntArray> = mutableMapOf()

    var uninitialized: Boolean = true
    lateinit var vm: VM
    var collector: TypeCollector? = null

    var readFunction: Function<String, String> = Function {
        val bytes = Files.readAllBytes(Paths.get(it))
        return@Function String(bytes, Charset.defaultCharset())
    }
    var natives: Natives = DefaultNatives

    fun start(): VM? {
        instance = this
        debug = false
        bytecodeDebug = false
        stacktrace = false
        warnStacktrace = false
        logToStdout = ranFromMain
        tickMode = false
        noTypeChecks = false
        compileOnly = false
        when {
            args.size > 4 -> {
                println("Usage: sunlite [script] (path) (options) (args)")
                exitProcess(64)
            }

            args.size == 1 -> {
                return runFile(args[0])
            }

            args.size == 2 -> {
                path.addAll(args[1].split(";"))
                return runFile(args[0])
            }

            args.size == 3 -> {
                args[2].split(";").forEach {
                    when (it) {
                        "debug" -> debug = true
                        "trace" -> bytecodeDebug = true
                        "stacktrace" -> stacktrace = true
                        "warnStacktrace" -> warnStacktrace = true
                        "stdout" -> logToStdout = true
                        "tick" -> tickMode = true
                        "noTypes" -> noTypeChecks = true
                        "compile" -> compileOnly = true
                    }
                }
                path.addAll(args[1].split(";"))
                return runFile(args[0])
            }

            args.size == 4 -> {
                args[2].split(";").forEach {
                    when (it) {
                        "debug" -> debug = true
                        "trace" -> bytecodeDebug = true
                        "stacktrace" -> stacktrace = true
                        "warnStacktrace" -> warnStacktrace = true
                        "stdout" -> logToStdout = true
                        "tick" -> tickMode = true
                        "noTypes" -> noTypeChecks = true
                        "compile" -> compileOnly = true
                    }
                }
                path.addAll(args[1].split(";"))
                return runFile(args[0])
            }

            else -> {
                runPrompt()
            }
        }
        return null
    }

    fun parse(code: String? = null): ParsedData? {
        instance = this
        compileStep = 0
        val filePath = args[0]
        path.addAll(args[1].split(";"))

        val data: String = code ?: readFunction.apply(filePath)

        val shortPath = filePath.split("/").last()

        val scanner = Scanner(data, this)
        val tokens: List<Token> = scanner.scanTokens(shortPath)

        val nativesObj = BasicNativesContainer()
        natives.registerNatives(nativesObj)

        collector = TypeCollector(this, nativesObj)

        var parser = Parser(tokens, this, true)
        var statements: MutableList<Stmt> = parser.parse(shortPath).toMutableList()

        //compileStep++

        // Stop if there was a syntax error.
        //if (hadError) return null

        collector?.collect(statements, shortPath, compileStep)

        compileStep++

        // Stop if there was a type collection error.
        //if (hadError) return null

        parser = Parser(tokens, this)
        statements = parser.parse(shortPath).toMutableList()

        collector?.collect(statements, shortPath, compileStep)

        compileStep++

        parser = Parser(tokens, this)
        statements = parser.parse(shortPath).toMutableList()

        compileStep++

        // Stop if there was a syntax error.
        //if (hadError) return null

        val allStatements: MutableList<Stmt> = mutableListOf()
        //includes.values.sortedBy { it.first }.reversed().forEach { allStatements.addAll(it.second) }
        allStatements.addAll(statements)

        // Stop if there was a type collection error.
        //if (hadError) return null

        if (!noTypeChecks) {
            val checker = TypeChecker(this, null)
            checker.check(allStatements)
        }

        compileStep++

        // Stop if there was a type error.
        //if (hadError) return null

        return ParsedData(tokens, allStatements, collector!!)
    }

    fun compile(statements: List<Stmt>): SLFunction {
        val filePath = args[0]
        path.addAll(args[1].split(";"))
        val shortPath = filePath.split("/").last()

        val compiler = Compiler(this, vm, null)
        return compiler.compile(
            FunctionType.FUNCTION,
            FunctionModifier.NORMAL,
            Type.NIL,
            listOf(),
            listOf(),
            statements,
            shortPath
        )
    }

    @Throws(IOException::class)
    private fun runFile(path: String): VM? {

        if(Path(path).extension != "sl" && Path(path).extension != "slc") {
            printErr("Source files must have .sl or .slc file extension!")
            return null
        }

	    if(Path(path).extension == "slc") {
            DataInputStream(GZIPInputStream(File(path).inputStream().buffered())).use { input ->
                vm = VM(this, if (args.size == 4) args[3].split(";").toTypedArray() else arrayOf())
                uninitialized = false
                val program: SLFunction = SLFunction.read(input)
                return run(program)
            }
	    }

        val VM = runString(readFunction.apply(path), path)

        // Indicate an error in the exit code.
        if (hadError) return null //exitProcess(65)
        if (hadRuntimeError) return null//exitProcess(70)

        return VM
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

    private fun runString(source: String, path: String?): VM? {
        return compile(source, path)
    }

    private fun compile(source: String, path: String?): VM? {
        if(debug){
            printInfo("Load path: ")
            printInfo("-----")
            this.path.forEach { printInfo(it) }
            printInfo("-----")
            printInfo()
        }

        compileStep = 0

        val shortPath = path?.split("/")?.last()

        val scanner = Scanner(source, this)
        val tokens: List<Token> = scanner.scanTokens(shortPath)

        if (debug) {
            printInfo("Tokens: ")
            printInfo("-----")
            tokens.forEach { printInfo("${it.lexeme} ${it.type}") }
            printInfo("-----")
            printInfo()
        }

        vm = VM(this, if (args.size == 4) args[3].split(";").toTypedArray() else arrayOf())
        uninitialized = false

        collector = TypeCollector(this, vm)

        var parser = Parser(tokens, this, true)
        var statements: MutableList<Stmt> = parser.parse(shortPath).toMutableList()

        //compileStep++

        // Stop if there was a syntax error.
        if (hadError) return null

        collector?.collect(statements, shortPath, compileStep)

        compileStep++

        // Stop if there was a type collection error.
        if (hadError) return null

        parser = Parser(tokens, this)
        statements = parser.parse(shortPath).toMutableList()

        //compileStep++

        // Stop if there was a syntax error.
        if (hadError) return null

        collector?.collect(statements, shortPath, compileStep)

        compileStep++

        parser = Parser(tokens, this)
        statements = parser.parse(shortPath).toMutableList()

        compileStep++

        // Stop if there was a type collection error.
        if (hadError) return null

        val allStatements: MutableList<Stmt> = mutableListOf()
        includes.values.sortedBy { it.first }.reversed().forEach { allStatements.addAll(it.second) }
        allStatements.addAll(statements)

        //collector = TypeCollector(this, vm)
        //collector?.collect(allStatements, shortPath)

       // compileStep++

        // Stop if there was a type collection error.
        if (hadError) return null

        if (debug) {
            printInfo("AST: ")
            printInfo("-----")
            statements.forEach {
                printInfo(AstPrinter.print(it))
            }
            printInfo("-----")
            printInfo()
        }

        if (debug) {
            printInfo("Type Collection: ")
            printInfo("--------")
            collector?.typeScopes?.forEach { printTypeScopes(it, 0) }
            printInfo("--------")
            printInfo()
            printInfo("Type Hierarchy: ")
            printInfo("--------")
            collector?.typeHierarchy?.forEach { printInfo("${it.key}<${it.value.typeParameters.joinToString()}> extends ${it.value.superclass} implements ${if (it.value.superinterfaces.isNotEmpty()) it.value.superinterfaces.joinToString() else "<nil>"}") }
            printInfo("--------")
            printInfo("--------")
            printInfo()

        }

        if (!noTypeChecks) {
            val checker = TypeChecker(this, vm)
            checker.check(statements)
        }

        compileStep++

        // Stop if there was a type error.
        if (hadError) return null

        val compiler = Compiler(this, vm, null)

        val program: SLFunction = compiler.compile(
            FunctionType.NONE,
            FunctionModifier.NORMAL,
            Type.NIL,
            listOf(),
            listOf(),
            allStatements,
            shortPath
        )

        compileStep++

        // Stop if there was a compilation error.
        if (hadError) return null

        if(compileOnly){
            if(path != null) {
                if(Path(path).extension == "sl"){
                    val compiledPath = path.replace(".sl",".raw")
                    val file = File(compiledPath)
                    file.createNewFile()
                    val stream = DataOutputStream(file.outputStream())
                    stream.use { program.write(it) }
                    CompressUtils.compress(Path(compiledPath), Path(compiledPath.replace(".raw", ".slc")),)
                    File(compiledPath).delete()
                    printInfo("Compiled ${Path(path).fileName}.")
                    return null
                } else {
                    printErr("File already compiled!")
                    return null
                }
            } else {
                printErr("Path for compilation output must be specified!")
                return null
            }
        }

        return run(program)
    }

    private fun run(program: SLFunction): VM? {
        vm.call(SLClosureObj(SLClosure(program)), 0)

        if (!(tickMode)) {
            try {
                vm.run()
            } catch (e: UnhandledException) {
                if (stacktrace) {
                    e.printStackTrace()
                }
                vm.printStacktrace(e.message ?: "null")
            } catch (e: Exception) {
                if (stacktrace) {
                    e.printStackTrace()
                }
                vm.runtimeError("InternalError: $e")
            }
            return null
        }

        return vm
    }

    fun printTypeScopes(it: TypeCollector.Scope?, depth: Int = 0) {
        if (it == null) return
        val sb = StringBuilder()
        sb.append("\t".repeat(depth))
        sb.append("${it.name.lexeme} {")
        it.contents.forEach {
            sb.append("\n")
            sb.append("\t".repeat(depth + 1))
            sb.append("${it.key.lexeme} = ${it.value}")
        }
        printInfo(sb.toString())
        it.inner.forEach { printTypeScopes(it, depth + 1) }
        sb.clear()
        sb.append("\t".repeat(depth))
        sb.append("}")
        printInfo(sb.toString())
    }


    fun error(line: Int, message: String, file: String? = null) {
        reportError(line, "", message, file ?: "<unknown file>")
    }

    fun error(token: Token, message: String) {
        dataReceiver.forEach { it.error(CompilerError(token, message)) }
        if (token.type == TokenType.EOF) {
            reportError(token.line, " at end", message, token.file, token.pos)
        } else {
            reportError(token.line, " at '" + token.lexeme + "'", message, token.file, token.pos)
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
        message: String, file: String?, position : Token.Position? = null
    ) {
        val s = "[$file, line $line${if(position != null) ", $position" else ""}] Error$where: $message"
        printErr(s)

        if (stacktrace) {
            throw CompilationException("sunlite compilation error:")
        }

        hadError = true
    }

    private fun reportWarn(
        line: Int, where: String,
        message: String, file: String?
    ) {
        val s = "[$file, line $line] Warn$where: $message"
        printWarn(s)

        if (warnStacktrace) {
            CompilationException("sunlite compilation warning stack trace").printStackTrace()
        }
    }

    fun printInfo(message: Any? = "") {
        if (logToStdout) println(message)
        logEntryReceivers.forEach { it.info(message.toString()) }
    }

    fun printWarn(message: Any? = "") {
        if (logToStdout) System.err.println(message)
        logEntryReceivers.forEach { it.warn(message.toString()) }
    }

    fun printErr(message: Any? = "") {
        if (logToStdout) System.err.println(message)
        logEntryReceivers.forEach { it.err(message.toString()) }
    }

    /*inner class DebuggerServer(val port: Int = 24128): Thread() {
        private val socket: ServerSocket = ServerSocket(port)
        val clients: MutableList<ClientHandler> = mutableListOf()

        inner class ClientHandler(val socket: Socket): Thread() {
            lateinit var reader: BufferedReader
            lateinit var writer: BufferedWriter

            //val input = socket.getInputStream()
            //val outputStream = socket.getOutputStream()

            //val reader = BufferedReader(InputStreamReader(input))
            //val writer = OutputStreamWriter(outputStream)

            override fun run(){
                reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                while(true){

                }
                /*reader.close()
                writer.close()
                socket.close()*/
            }

        }

        override fun run() {
            while (true){
                val clientSocket = socket.accept()
                val clientHandler = ClientHandler(clientSocket)
                clients.add(clientHandler)
                clientHandler.start()
            }
        }

        fun close(){
            socket.close()
        }
    }*/

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
        var tickMode = false

        @JvmStatic
        var noTypeChecks = false

        @JvmStatic
        var ranFromMain = false

        @JvmStatic
        var compileOnly = false

        lateinit var instance: Sunlite

        @JvmStatic
        @Throws(IOException::class)
        fun main(args: Array<String>) {
            instance = Sunlite(args)
            ranFromMain = true
            instance.start()
        }
    }
}