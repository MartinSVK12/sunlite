package sunsetsatellite.lsp.sunlite

import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.Range
import sunsetsatellite.lang.sunlite.CompilerDataReceiver
import sunsetsatellite.lang.sunlite.CompilerError
import sunsetsatellite.lang.sunlite.Stmt
import sunsetsatellite.lang.sunlite.Sunlite
import sunsetsatellite.lang.sunlite.Token
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Path
import kotlin.concurrent.thread

class SunliteCodeAnalysis(val languageServer: SunliteLanguageServer, val documentUri: String?): CompilerDataReceiver {

	companion object {
		var inProgress: Boolean = false
		var lastAnalysis: Pair<List<Token>,List<Stmt>>? = null
	}

	private val errors: MutableList<CompilerError> = ArrayList()
	private var thread: Thread? = null

	fun analysisFinished(result: Pair<List<Token>,List<Stmt>>? = null){
		inProgress = false
		lastAnalysis = result
		val diagnostics: MutableList<Diagnostic> = mutableListOf()

		errors.forEach {
			diagnostics.add(Diagnostic(
				Range(Position(it.token.line-1,it.token.pos.start), Position(it.token.line-1,it.token.pos.end)),
				it.message,
				DiagnosticSeverity.Error,
				"Sunlite"
			))
		}

		languageServer.client.publishDiagnostics(PublishDiagnosticsParams(
			documentUri,
			diagnostics
		))
	}

	fun analyze(file: Path, loadPath: Array<Path> = arrayOf(), code: String? = null) {
		if(inProgress) return
		inProgress = true
		thread = thread(
			start = true,
			name = "Sunlite Code Analysis Thread",
		) {
			val sl = Sunlite(arrayOf(file.toString(), loadPath.joinToString(";")))
			sl.dataReceiver.add(this)
			val result = sl.parse(code)
			if(result == null){
				this.analysisFinished()
				return@thread
			} else {
				this.analysisFinished(result.tokens to result.statements)
			}
		}
		thread!!.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler {
			_, e ->
			if(e is ThreadDeath) return@UncaughtExceptionHandler
			val sw = StringWriter()
			e.printStackTrace(PrintWriter(sw))
			val s = sw.toString()
			errors.add(CompilerError(Token.identifier("<internal error>"),s))
			this.analysisFinished()
		}
	}

	override fun error(error: CompilerError) {
		errors.add(error)
	}
}