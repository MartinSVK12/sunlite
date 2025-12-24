package sunsetsatellite.lsp.sunlite

import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.DidSaveTextDocumentParams
import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.HoverParams
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.TextDocumentService
import java.net.URI
import java.util.concurrent.CompletableFuture
import kotlin.io.path.Path

class SunliteTextDocumentService(val languageServer: SunliteLanguageServer): TextDocumentService {
	override fun didOpen(open: DidOpenTextDocumentParams?) {
		val uri = open?.textDocument?.uri ?: return
		val path = Path(URI(uri).path.replaceFirst("/",""))
		//languageServer.client.showMessage(MessageParams(MessageType.Info,path.toString()))
		SunliteCodeAnalysis(languageServer, open.textDocument.uri).analyze(path, arrayOf(path.parent,path.parent.resolve("stdlib")), open.textDocument.text)
		//languageServer.client.showMessage(MessageParams(MessageType.Info,open?.textDocument?.uri))
		/*languageServer.client.publishDiagnostics(PublishDiagnosticsParams(open!!.textDocument.uri, listOf(
			Diagnostic(Range(Position(0, 0), Position(0, 5)), "Testing LSP with one click build!", DiagnosticSeverity.Error, "Sunlite")
		)))*/
	}

	override fun didChange(change: DidChangeTextDocumentParams?) {
		val text = change?.contentChanges?.first()?.text
		val uri = change?.textDocument?.uri ?: return
		val path = Path(URI(uri).path.replaceFirst("/",""))
		SunliteCodeAnalysis(languageServer, change.textDocument.uri).analyze(path, arrayOf(path.parent,path.parent.resolve("stdlib")), text)
	}

	override fun didClose(close: DidCloseTextDocumentParams?) {

	}

	override fun didSave(save: DidSaveTextDocumentParams?) {

	}

	/*override fun hover(params: HoverParams?): CompletableFuture<Hover?>? {
		if (params == null) return null
		return CompletableFuture.completedFuture(Hover(Either.forLeft("Hovering over ${params.position}!")))
	}*/
}