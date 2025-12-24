package sunsetsatellite.lsp.sunlite

import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageClient

object LSPLauncher {

	@JvmStatic
	fun main(args: Array<String>) {
		val server = SunliteLanguageServer()
		val launcher: Launcher<LanguageClient> = LSPLauncher.createServerLauncher(server, System.`in`, System.out)
		server.client = launcher.remoteProxy
		println("Sunlite LSP server started!")
		launcher.startListening()
	}
}