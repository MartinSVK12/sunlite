package sunsetsatellite.sunlite.lang

import sunsetsatellite.sunlite.vm.*
import java.io.*
import java.util.function.Function
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.system.exitProcess
import kotlin.time.measureTime

class Sunlite(val args: Array<String>) {

    var hadError: Boolean = false
    var hadRuntimeError: Boolean = false
    var compileStep: Int = 0

    val path: MutableList<String> = mutableListOf()
    val includes: MutableMap<String, Pair<Int, List<Stmt>>> = mutableMapOf()
    val imports: MutableMap<String, Pair<Int, List<Stmt>?>> = mutableMapOf()
    val autoImported: MutableMap<String, List<String>> = mutableMapOf()

    val logEntryReceivers: MutableList<LogEntryReceiver> = mutableListOf()
    val compilerDataReceivers: MutableList<CompilerDataReceiver> = mutableListOf()
    val breakpointListeners: MutableList<BreakpointListener> = mutableListOf()

    var breakpoints: MutableMap<String, IntArray> = mutableMapOf()

    var uninitialized: Boolean = true
    lateinit var vm: VM
    var collector: TypeCollector? = null

    // file path -> file contents as text
    var readFunction: Function<String, String> = Function {
        return@Function readStreamFunction.apply(it).use { s -> s.bufferedReader().readText() }
    }

    // file path -> input stream
    var readStreamFunction: Function<String, InputStream> = Function {
        return@Function File(it).inputStream()
    }

    // abstract module path -> file path
    var modulePathReadFunction: Function<String, String> = Function {
        val path = "/" + it.replace("::","/") + ".sl"
        return@Function path
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
        compileDeps = false
        showDisassembly = false
        showOtherDisassembly = false
        showAST = false
        showTypeCollection = false
        showTokens = false
	    hadError = false
	    hadRuntimeError = false
	    compileStep = 0
	    path.clear()
	    includes.clear()
	    imports.clear()
        autoImported.clear()
	    uninitialized = true
	    collector = null

        autoImported["sunlite::stdlib::object"] = listOf("Object")
        autoImported["sunlite::stdlib::exception"] = listOf("Exception")
        autoImported["sunlite::stdlib::string"] = listOf("Strings")
        autoImported["sunlite::stdlib::enums"] = listOf("Enum")
        autoImported["sunlite::stdlib::array"] = listOf("Arrays", "ArrayIterator")

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
                        "compileDeps" -> compileDeps = true
                        "asm" -> showDisassembly = true
                        "otherAsm" -> showOtherDisassembly = true
                        "ast" -> showAST = true
                        "otherAst" -> showOtherAST = true
                        "types" -> showTypeCollection = true
                        "tokens" -> showTokens = true
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
                        "compileDeps" -> compileDeps = true
                        "asm" -> showDisassembly = true
                        "otherAsm" -> showOtherDisassembly = true
                        "ast" -> showAST = true
                        "otherAst" -> showOtherAST = true
                        "types" -> showTypeCollection = true
                        "tokens" -> showTokens = true
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

    fun parse(code: String? = null): ParsedData {
        instance = this
        compileStep = 0

        val filePath = args[0]
        path.addAll(args[1].split(";"))

        val data: String = code ?: readFunction.apply(filePath)

        val scanner = Scanner(data, this)
        val tokens: List<Token> = scanner.scanTokens(filePath)

        val nativesObj = BasicNativesContainer()
        natives.registerNatives(nativesObj)

        collector = TypeCollector(this, nativesObj)

        var parser = Parser(tokens, this, true)
        var statements: MutableList<Stmt> = parser.start(filePath).toMutableList()

        repeat(MAX_COMPILE_STEP) {
            collector?.collect(statements, filePath, compileStep)
            compileStep++
            imports.clear()

            parser = Parser(tokens, this, true)
            statements = parser.start(filePath).toMutableList()
        }

        val allStatements: MutableList<Stmt> = mutableListOf()
        includes.values.sortedBy { it.first }.reversed().forEach { allStatements.addAll(it.second) }
        allStatements.addAll(statements)


        // Stop if there was a type collection error.
        //if (hadError) return null

        if (!noTypeChecks) {
            val checker = TypeChecker(this, null)
            checker.check(statements)
        }

        compileStep++

        // Stop if there was a type error.
        //if (hadError) return null

        val compiler = Compiler(this, null, null)

        val program: SLFunction = compiler.compile(
            FunctionType.CHUNK,
            arrayOf(FunctionModifier.CHUNK),
            Type.NIL,
            listOf(),
            listOf(),
            allStatements,
	        filePath
        )

        compileStep++

        // Stop if there was a compilation error.
        //if (hadError) return null

        return ParsedData(tokens, allStatements, collector!!)
    }

    fun compile(statements: List<Stmt>): SLFunction {
        val filePath = args[0]
        path.addAll(args[1].split(";"))

        val compiler = Compiler(this, vm, null)
        return compiler.compile(
            FunctionType.FUNCTION,
            arrayOf(FunctionModifier.NORMAL),
            Type.NIL,
            listOf(),
            listOf(),
            statements,
            filePath
        )
    }

    @Throws(IOException::class)
    private fun runFile(path: String): VM? {

        if(Path(path).extension != "sl" && Path(path).extension != "slc") {
            printErr("Source files must have .sl or .slc file extension!")
            return null
        }

	    if(Path(path).extension == "slc") {
            //printErr("Running compiled files is not supported yet!")
            DataInputStream(File(path).inputStream().buffered()).use { input ->
                vm = VM(this, if (args.size == 4) args[3].split(";").toTypedArray() else arrayOf())
                vm.compiled = true
                uninitialized = false
                collector = TypeCollector(this, vm)
                val program: SLFunction = SLFunction.read(input)
                return run(program)
            }
            //return null
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
        return runOrCompile(source, path)
    }

    private fun runOrCompile(source: String, path: String?): VM? {
        var run = true
        if(compileOnly){
            if(path == null){
                printErr("Path for compilation output must be specified!")
                return null
            }
            run = false
        }

        if(debug){
            printDebug("Load path: ")
            printDebug("-----")
            this.path.forEach { printDebug(it) }
            printDebug("-----")
            printDebug()
        }

        val program: SLFunction

        val duration = measureTime {
            compileStep = 0

            val scanner = Scanner(source, this)
            val tokens: List<Token> = scanner.scanTokens(path)

            if (showTokens) {
                printDebug("Tokens: ")
                printDebug("-----")
                tokens.forEach { printDebug("${it.lexeme} ${it.type}") }
                printDebug("-----")
                printDebug()
            }

            vm = VM(this, if (args.size == 4) args[3].split(";").toTypedArray() else arrayOf())
            uninitialized = false

            collector = TypeCollector(this, vm)

            var parser = Parser(tokens, this, true)
            var statements: MutableList<Stmt> = parser.start(path).toMutableList()

            // Stop if there was a syntax error.
            if (hadError) return null

            repeat(MAX_COMPILE_STEP) {
                collector?.collect(statements, path, compileStep)
                compileStep++
                imports.clear()

                parser = Parser(tokens, this, true)
                statements = parser.start(path).toMutableList()

                // Stop if there was a syntax error.
                if (hadError) return null
            }

            val allStatements: MutableList<Stmt> = mutableListOf()
            includes.values.sortedBy { it.first }.reversed().forEach { allStatements.addAll(it.second) }
            allStatements.addAll(statements)

            if (showTypeCollection) {
                printDebug("Type Collection: ")
                printDebug("--------")
                collector?.typeScopes?.forEach { printTypeScopes(it, 0) }
                printDebug("--------")
                printDebug()
                printDebug("Type Hierarchy: ")
                printDebug("--------")
                collector?.typeHierarchy?.forEach { printDebug("${it.key}<${it.value.typeParameters.joinToString()}> extends ${it.value.superclass} implements ${if (it.value.superinterfaces.isNotEmpty()) it.value.superinterfaces.joinToString() else "<nil>"}") }
                printDebug("--------")
                printDebug()
            }

            if (showAST) {
                printDebug("AST: ${path}")
                printDebug("-----")
                statements.forEach {
                    printDebug(AstPrinter.print(it))
                }
                printDebug("-----")
                printDebug()
            }

            if (!noTypeChecks) {
                val checker = TypeChecker(this, vm)
                checker.check(statements)
            }

            compileStep++

            // Stop if there was a type error.
            if (hadError) return null

            val compiler = Compiler(this, vm, null)

            val modules: MutableMap<String, SLModuleObj> = mutableMapOf()

            imports.forEach { import ->
                val paths = import.key.split("::").toMutableList()
                var name = paths.removeLast()
                if(name == "*"){
                    name = paths.removeLast()
                }
                val path = paths.joinToString("::")
                val func = Compiler(this, vm, null)
                    .compileModule(
                        import.value.second!!,
                        path,
                        "<module '$path::$name'>"
                    )
                func.chunk.debugInfo.classData.forEach { (string, data) ->
                    vm.classes[string] = data
                }
                if(path.isEmpty()){
                    modules[name] = SLModuleObj(SLModule(
                        name,
                        Path("."),
                        false,
                        initializer = SLClosureObj(SLClosure(func))
                    ))
                }
                if(paths.isNotEmpty()){
                    val p = mutableListOf<String>()
                    var current: SLModule = modules.computeIfAbsent(paths.first()) {
                        SLModuleObj(SLModule(
                            paths.first(),
                            Path("."),
                            true
                        ))
                    }.value
                    p.add(paths.removeFirst())
                    paths.forEach {
                        p.add(it)
                        current = current.env.computeIfAbsent(it) {
                            SLModuleObj(SLModule(
                                it,
                                Path(p.joinToString("/")),
                                true
                            ))
                        }.value as SLModule
                    }
                    p.add(name)
                    current.env.computeIfAbsent(name) {
                        SLModuleObj(SLModule(
                            name,
                            Path(p.joinToString("/")),
                            false,
                            initializer = SLClosureObj(SLClosure(func))
                        ))
                    }
                }
                if(compileDeps){
                    val dir = Path(".", "out", path.replace("::","/"), name).toFile()
                    val file = Path(".", "out", path.replace("::","/"), name, "$name.slc").toFile()
                    dir.mkdirs()
                    file.createNewFile()
                    val stream = DataOutputStream(file.outputStream())
                    stream.use { s -> func.write(s) }
                    printInfo("Exported $file")
                }
            }

            vm.globals.putAll(modules)

            if(debug){
                printDebug()
                printDebug("Imported Modules: ")
                printDebug("--------")
                imports.forEach {
                    printDebug(it.key)
                }
                printDebug("--------")
            }

            program = compiler.compile(
                FunctionType.CHUNK,
                arrayOf(FunctionModifier.CHUNK),
                Type.NIL,
                listOf(),
                listOf(),
                allStatements,
                path
            )

            program.chunk.debugInfo.classData.forEach { (name, data) ->
                vm.classes[name] = data
            }

            compileStep++

            // Stop if there was a compilation error.
            if (hadError) return null

            if(compileOnly){
                if(path != null) {
                    if(Path(path).extension == "sl"){
                        val dir = Path(".", "out").toFile()
                        val file = Path(".", "out", "${Path(path).fileName}c").toFile()
                        dir.mkdirs()
                        file.createNewFile()
                        file.createNewFile()
                        val stream = DataOutputStream(file.outputStream())
                        stream.use { program.write(it) }
                        //CompressUtils.compress(Path(compiledPath), Path(compiledPath.replace(".slc", ".slcc")))
                        //File(compiledPath).delete()
                        printInfo("Exported $file")
                        run = false
                    }
                }
            }
        }
        if(debug){
            printInfo("Compilation completed after ${duration}.")
            printDebug()
        }
	    return if (run) run(program) else null
    }

    private fun run(program: SLFunction): VM? {
        vm.call(SLClosureObj(SLClosure(program)), 0)

        if (!(tickMode)) {
            val duration = measureTime {
                try {
                    vm.run()
                } catch (e: UnhandledException) {
                    if (stacktrace) {
                        e.printStackTrace()
                    }
                    vm.printStacktrace(e.e)
                } catch (e: VMError) {
                    e.printStackTrace()
                } catch (e: Exception) {
                    if (stacktrace) {
                        e.printStackTrace()
                    }
                    vm.printStacktrace("InternalError: $e")
                }
            }
            if(debug){
                printDebug()
                printInfo("Execution finished after ${duration}, number of instructions ran: ${vm.instCounter}")
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
        printDebug(sb.toString())
        it.inner.forEach { printTypeScopes(it, depth + 1) }
        sb.clear()
        sb.append("\t".repeat(depth))
        sb.append("}")
        printDebug(sb.toString())
    }


    fun error(line: Int, message: String, file: String? = null) {
        reportError(line, "", message, file ?: "<unknown file>")
    }

    fun error(token: Token, message: String) {
        compilerDataReceivers.forEach { it.error(CompilerError(token, message)) }
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

        /*if (stacktrace) {
            CompilationException("sunlite compilation error:").printStackTrace()
        }*/

        hadError = true
    }

    private fun reportWarn(
        line: Int, where: String,
        message: String, file: String?
    ) {
        val s = "[$file, line $line] Warn$where: $message"
        printWarn(s)

        /*if (warnStacktrace) {
            CompilationException("sunlite compilation warning stack trace").printStackTrace()
        }*/
    }

    fun printInfo(message: Any? = "") {
        if (logToStdout) println("\u001b[39m$message\u001b[39m")
        logEntryReceivers.forEach { it.info(message.toString()) }
    }

    fun printDebug(message: Any? = ""){
        if (logToStdout) println("\u001b[90m$message\u001b[39m")
        logEntryReceivers.forEach { it.debug(message.toString()) }
    }

    fun printWarn(message: Any? = "") {
        if (logToStdout) println("\u001b[33m$message\u001b[39m")
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
        var showTypeCollection: Boolean = false

        @JvmStatic
        var showAST: Boolean = false

        @JvmStatic
        var showTokens: Boolean = false

        @JvmStatic
        var showDisassembly: Boolean = false

        @JvmStatic
        var showOtherDisassembly: Boolean = false

        @JvmStatic
        var showOtherAST: Boolean = false

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

        @JvmStatic
        var compileDeps = false

        const val MAX_COMPILE_STEP = 10;

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