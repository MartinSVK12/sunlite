package sunsetsatellite.sunlite.lang

interface CompilerDataReceiver {
	fun error(error: CompilerError)
}

data class CompilerError(val token: Token, val message: String)