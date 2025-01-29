package sunsetsatellite.lang.lox

class LoxRuntimeError(val token: Token, message: String): RuntimeException(message) {
}