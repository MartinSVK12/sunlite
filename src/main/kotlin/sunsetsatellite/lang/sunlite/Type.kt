package sunsetsatellite.lang.sunlite

import sunsetsatellite.vm.sunlite.SLArray
import sunsetsatellite.vm.sunlite.SLClass
import sunsetsatellite.vm.sunlite.SLClassInstance
import sunsetsatellite.vm.sunlite.SLFunction

abstract class Type {

    open class Singular(val type: PrimitiveType, val ref: String = "") : Type() {

        override fun getName(): String {
            return type.name.lowercase()
        }

        override fun toString(): String {
            return getName()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other != null) {
                if (other !is Singular) return false
            } else {
                return false
            }

            if (type != other.type) return false
            if (ref != other.ref && ref != "") return false

            return true
        }

        override fun hashCode(): Int {
            var result = type.hashCode()
            result = 31 * result + ref.hashCode()
            return result
        }
    }

    class Reference(type: PrimitiveType, ref: String, val returnType: Type, val params: List<Param> = listOf()) : Singular(type, ref) {
        override fun getName(): String {
            return ref
        }

        override fun toString(): String {
            if(type == PrimitiveType.FUNCTION){
                return "${type.name.lowercase()} '${ref}(${params.map { it.type }.joinToString()}): ${returnType}'"
            } else {
                return "${type.name.lowercase()} '${ref}'"
            }
        }

        override fun equals(other: Any?): Boolean {
            if(other !is Reference) return false
            if(other.type != type) return false
            if(this.type == other.type && this.returnType == other.returnType) {
                if(this.params.size != other.params.size) return false
                val types = params.map { it.type }
                val otherTypes = other.params.map { it.type }
                if(types.zip(otherTypes).any { !it.first.equals(it.second) }) return false
                return true
            }
            return false
        }

        override fun hashCode(): Int {
            var result = type.hashCode()
            result = 31 * result + ref.hashCode()
            return result
        }

    }

    class Union(val types: List<Singular>) : Type() {
        override fun getName(): String {
            return types.joinToString(" | ")
        }

        override fun toString(): String {
            return getName()
        }
    }

    class Parameter(val name: Token) : Type() {
        override fun getName(): String {
            return "<${name.lexeme}>"
        }

        override fun toString(): String {
            return getName()
        }
    }

    abstract fun getName(): String

    companion object {

        fun ofClass(name: String): Reference {
            return Reference(PrimitiveType.CLASS, name, ofObject(name), listOf())
        }

        fun ofFunction(name: String, returnType: Type, params: List<Param>): Reference {
            return Reference(PrimitiveType.FUNCTION, name, returnType, params)
        }

        fun ofObject(name: String): Reference {
            val reference = let {
                val reference = Reference(PrimitiveType.OBJECT, name, OBJECT, listOf())
                Reference(PrimitiveType.OBJECT, name, reference, listOf())
            }

            return reference
        }

        fun of(tokens: List<TypeToken>): Type {
            if(tokens.size == 1){
                if(tokens[0].pure){
                    return Parameter(tokens[0].token)
                }
                if(tokens[0].token.type != TokenType.IDENTIFIER){
                    if(tokens[0].token.type == TokenType.CLASS){
                        return ofClass(tokens[0].token.lexeme)
                    }
                    if(tokens[0].token.type == TokenType.TYPE_FUNCTION){
                        val tokenParams = tokens[0].typeParameters
                        val params = if(tokenParams.size < 2) listOf() else tokenParams.map { Param(Token.identifier(""), of(listOf(it))) }
                        return ofFunction("", if(tokenParams.isEmpty()) NIL else of(listOf(tokenParams.last())), params)
                    }
                    return Singular(PrimitiveType.get(tokens[0].token))
                } else {
                    return ofObject(tokens[0].token.lexeme)
                }
            } else if (tokens.size > 1) {
                val types: List<Singular> = tokens.map { of(listOf(it)) } as List<Singular>
                return Union(types)
            } else {
                throw IllegalArgumentException("No types provided.")
            }
        }

        fun contains(type: Type, inType: Type): Boolean {
            if(type == UNKNOWN) return true //can't statically determine the type
            if(inType is Union){
                if(type is Union) {
                    return inType.types.containsAll(type.types) || inType.types.contains(ANY)
                } else {
                    return inType.types.any { it.equals(type) } || inType.types.contains(ANY)
                }
            } else {
                return inType.equals(type) || inType == ANY
            }
        }

        fun fromValue(value: Any?, sunlite: Sunlite): Type {
            return when (value) {
                is Type -> value
                is Param -> value.type
                is String -> STRING
                is Double -> NUMBER
                is Boolean -> BOOLEAN
                null -> NIL
                is SLFunction -> ofFunction(value.name, value.returnType, value.params)
                is SLClass -> ofClass(value.name)
                //is LoxInterface -> ofClass(value.name, sunlite)
                is SLClassInstance -> ofObject(value.clazz.name)
                is SLArray -> ARRAY
                else -> UNKNOWN
            }
        }

        val UNKNOWN = Singular(PrimitiveType.UNKNOWN)
        val NIL = Singular(PrimitiveType.NIL)
        val ANY = Singular(PrimitiveType.ANY)
        val CLASS = Singular(PrimitiveType.CLASS)
        val FUNCTION = Singular(PrimitiveType.FUNCTION)
        val OBJECT = Singular(PrimitiveType.OBJECT)
        val ARRAY = Singular(PrimitiveType.ARRAY)
        val NULLABLE_ANY = Union(listOf(ANY, NIL))
        val NUMBER = Singular(PrimitiveType.NUMBER)
        val STRING = Singular(PrimitiveType.STRING)
        val BOOLEAN = Singular(PrimitiveType.BOOLEAN)
    }
}