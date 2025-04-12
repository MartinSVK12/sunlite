package sunsetsatellite.lang.lox

interface Element {
	fun getLine(): Int
	fun getFile(): String?
}