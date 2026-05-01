package sunsetsatellite.sunlite.vm

import sunsetsatellite.sunlite.lang.Token

class CompilationException(override val message: String, val token: Token? = null) : Exception(message)