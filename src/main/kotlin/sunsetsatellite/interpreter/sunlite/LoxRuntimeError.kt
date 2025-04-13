package sunsetsatellite.interpreter.sunlite

import sunsetsatellite.lang.sunlite.Token

class LoxRuntimeError(val token: Token, message: String): RuntimeException(message) {
}