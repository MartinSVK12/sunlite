package sunsetsatellite.sunlite.lang

class Descriptor(private val source: String) {
    private var current = 0
    private var totalChars = 0

    fun getType(): Type {
        return scanInner()
    }

    private fun isAtEnd(): Boolean {
        return current >= source.length
    }

    private fun scanInner(): Type {
        val list: MutableList<Type.Singular> = mutableListOf()
        while (!isAtEnd()) {
            list.add(scanDescriptor())
            if(peek() == '|'){
                advance()
            } else {
                break
            }
        }
        return if(list.size == 1) list[0] else Type.Union(list)
    }

    private fun scanDescriptor(): Type.Singular {
        val c = advance()
        val primitiveType = PrimitiveType.get(c)
        var currentType: Type.Singular = Type.Singular(primitiveType)
        if(primitiveType.isReference) {
            when (primitiveType) {
                PrimitiveType.OBJECT -> {
                    val s = identifier()
                    currentType = Type.ofObject(s)
                    advance()
                }

                PrimitiveType.CLASS -> {
                    val s = identifier()
                    currentType = Type.ofClass(s)
                    advance()
                }

                PrimitiveType.ARRAY -> {
                    currentType = Type.ofArray(scanInner())
                    advance()
                }

                PrimitiveType.TABLE -> {
                    val key = scanInner()
                    advance()
                    val value = scanInner()
                    currentType = Type.ofTable(key, value)
                }

                PrimitiveType.FUNCTION -> {
                    val params: MutableList<Type> = mutableListOf()
                    if(peek() != ')'){
                        do {
                        	params.add(scanInner())
                        } while (peek() != ')')
                    }
                    advance()
                    val returnType: Type = scanInner()
                    currentType = Type.ofFunction("",returnType,params.map { Param(Token.identifier(""), it) })
                    advance()
                }

                else -> {}
            }
        }
        return currentType
    }

    private fun match(expected: Char): Boolean {
        if (isAtEnd()) return false
        if (source[current] != expected) return false

        current++
        totalChars++
        return true
    }

    private fun peek(): Char {
        if (isAtEnd()) return '\u0000'
        return source[current]
    }

    private fun peekNext(): Char {
        if (current + 1 >= source.length) return '\u0000'
        return source[current + 1]
    }


    private fun advance(): Char {
        totalChars++
        return source[current++]
    }

    private fun identifier(): String {
        val start = current
        while (isAlphaNumeric(peek())) advance()
        return source.substring(start, current)
    }

    private fun isAlpha(c: Char): Boolean {
        return (c in 'a'..'z') ||
                (c in 'A'..'Z') || c == '_'
    }

    private fun isAlphaNumeric(c: Char): Boolean {
        return isAlpha(c) || c in '0'..'9'
    }
}