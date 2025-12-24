package sunsetsatellite.lang.sunlite

interface CompilerDataReceiver {
	fun error(error: CompilerError)
}

data class CompilerError(val token: Token, val message: String)