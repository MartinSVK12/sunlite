package sunsetsatellite.lsp.sunlite

import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.TextDocumentSyncKind
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.services.TextDocumentService
import org.eclipse.lsp4j.services.WorkspaceService
import java.util.concurrent.CompletableFuture
import kotlin.system.exitProcess

class SunliteLanguageServer: LanguageServer {

	lateinit var client: LanguageClient
	val textDocumentService = SunliteTextDocumentService(this)

	override fun initialize(p0: InitializeParams?): CompletableFuture<InitializeResult?>? {
		val capabilities = ServerCapabilities()
		capabilities.setTextDocumentSync(TextDocumentSyncKind.Full)
		val result = InitializeResult(capabilities)
		return CompletableFuture.completedFuture(result)
	}

	override fun shutdown(): CompletableFuture<in Any>? {
		return null
	}

	override fun exit() {
		exitProcess(0)
	}

	override fun getTextDocumentService(): TextDocumentService {
		return textDocumentService
	}

	override fun getWorkspaceService(): WorkspaceService? {
		return null
	}
}