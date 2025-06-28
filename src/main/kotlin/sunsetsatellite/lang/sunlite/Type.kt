package sunsetsatellite.lang.sunlite

import sunsetsatellite.vm.sunlite.SLArray
import sunsetsatellite.vm.sunlite.SLBoundMethod
import sunsetsatellite.vm.sunlite.SLClass
import sunsetsatellite.vm.sunlite.SLClassInstance
import sunsetsatellite.vm.sunlite.SLClosure
import sunsetsatellite.vm.sunlite.SLFunction
import sunsetsatellite.vm.sunlite.SLNativeFunction
import sunsetsatellite.vm.sunlite.SLType
import sunsetsatellite.vm.sunlite.SLUpvalue
import kotlin.math.sin

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

    class Reference(type: PrimitiveType, ref: String, val returnType: Type, val params: List<Param> = listOf(), val typeParams: List<Type> = listOf()) : Singular(type, ref) {
        override fun getName(): String {
            return ref
        }

        override fun toString(): String {
            when (type) {
                PrimitiveType.FUNCTION -> {
                    return "${type.name.lowercase()}${if(typeParams.isNotEmpty()) "<${typeParams.joinToString()}>" else ""} '${ref}(${params.map { it.type }.joinToString()}): ${returnType}'"
                }
                PrimitiveType.ARRAY -> {
                    return "${type.name.lowercase()} '${returnType}'"
                }
                PrimitiveType.OBJECT -> {
                    return "${type.name.lowercase()}${if(typeParams.isNotEmpty()) "<${typeParams.joinToString()}>" else ""}${if (ref != "") " '${ref}'" else ""}"
                }
                else -> {
                    return "${type.name.lowercase()}${if (ref != "") " '${ref}'" else ""}"
                }
            }
        }

        override fun equals(other: Any?): Boolean {
            if(other !is Reference) return false
            if(other.type != type) return false
            when (this.type) {
                PrimitiveType.FUNCTION -> {
                    if(this.returnType == other.returnType) {
                        if(this.params.size != other.params.size) return false
                        val types = params.map { it.type }
                        val otherTypes = other.params.map { it.type }
                        if(types.zip(otherTypes).any { !contains(it.second, it.first, currentInterpreter!!) }) return false
                        return true
                    }
                }
                PrimitiveType.OBJECT -> {
                    if(currentInterpreter == null) return false
                    var currentRef = other.ref
                    if(ref == currentRef) return true
                    while (currentRef != "<nil>"){
                        val superclass = currentInterpreter?.collector?.typeHierarchy[currentRef]
                        if(superclass == null || superclass == "<nil>" || superclass == "") return false
                        if(superclass == this.ref) return true
                        currentRef = superclass
                    }
                    return false
                }
                PrimitiveType.ARRAY -> {
                    if(contains(other.returnType, returnType, currentInterpreter!!)) return true
                    return false
                }
                PrimitiveType.CLASS -> {
                    if(this.ref == "") return true
                    if(this.ref == other.ref) return true
                }
                else -> {
                    return true
                }
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
            return "type parameter '${name.lexeme}'"
        }

        override fun toString(): String {
            return getName()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Parameter) return false

            if (name.lexeme != other.name.lexeme) return false

            return true
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }


    }

    abstract fun getName(): String

    companion object {

        var currentInterpreter: Sunlite? = null

        fun ofClass(name: String, params: List<Param> = listOf()): Reference {
            return Reference(PrimitiveType.CLASS, name, ofObject(name), params)
        }

        fun ofArray(elementType: Type): Reference {
            return Reference(PrimitiveType.ARRAY, "<array>", elementType, listOf())
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

        fun ofGenericObject(name: String, typeParams: List<Type>): Reference {
            val reference = let {
                val reference = Reference(PrimitiveType.OBJECT, name, OBJECT, listOf(), typeParams)
                Reference(PrimitiveType.OBJECT, name, reference, listOf(), typeParams)
            }

            return reference
        }

        fun ofGenericFunction(name: String, returnType: Type, params: List<Param>, typeParams: List<Type>): Reference {
            return Reference(PrimitiveType.FUNCTION, name, returnType, params, typeParams)
        }

        fun of(tokens: List<TypeToken>, topmost: Boolean = true): Type {
            if(tokens.isEmpty()) {
                throw IllegalArgumentException("No types provided.")
            } else if(tokens.size > 1 && topmost) {
                throw IllegalArgumentException("There can be only one topmost type.")
            } else {
                if(topmost){
                    val topmostType = tokens.first()
                    if(topmostType.tokens.size == 1) {
                        val singleToken = topmostType.tokens.entries.first()
                        when (singleToken.key.type) {
                            TokenType.TYPE_FUNCTION -> {
                                val returnType = if(topmostType.typeParameters.isEmpty()) Type.NIL else of(listOf(topmostType.typeParameters.last()), false)
                                val paramTokens = topmostType.typeParameters.dropLast(1)
                                val params = paramTokens.map { Param(Token.identifier(""), of(listOf(it), false)) }
                                return ofFunction("", returnType, params)
                            }
                            TokenType.TYPE_ARRAY -> {
                                val elementType = if(topmostType.typeParameters.isEmpty()) NULLABLE_ANY else of(topmostType.typeParameters, false)
                                return ofArray(elementType)
                            }
                            TokenType.CLASS -> {
                                if(topmostType.typeParameters.isEmpty()) {
                                    return ofClass("")
                                }
                                val className =
                                    topmostType.typeParameters.first().tokens.entries.first().key.lexeme
                                return ofClass(className)
                            }
                            TokenType.IDENTIFIER -> {
                                if(topmostType.typeParameters.isNotEmpty()) {
                                    val types = topmostType.typeParameters.map { of(listOf(it), false) }
                                    return ofGenericObject(singleToken.key.lexeme, types)
                                }
                                return ofObject(singleToken.key.lexeme)
                            }
                            TokenType.TYPE_GENERIC -> {
                                return Parameter(topmostType.typeParameters.first().tokens.keys.first())
                            }
                            else -> {
                                return Singular(PrimitiveType.get(singleToken.key))
                            }
                        }
                    } else {
                        val types = topmostType.tokens.values.map { of(it, false) as Singular }
                        return Union(types)
                    }
                } else {
                    if(tokens.size == 1) {
                        val singleType = tokens.first()
                        if(singleType.tokens.size == 1) {
                            val singleToken = singleType.tokens.entries.first()
                            when (singleToken.key.type) {
                                TokenType.TYPE_FUNCTION -> {
                                    val returnType = if(singleType.typeParameters.isEmpty()) Type.NIL else of(listOf(singleType.typeParameters.last()), false)
                                    val paramTokens = singleType.typeParameters.dropLast(1)
                                    val params = paramTokens.map { Param(Token.identifier(""), of(listOf(it), false)) }
                                    return ofFunction("", returnType, params)
                                }
                                TokenType.TYPE_ARRAY -> {
                                    val elementType = if(singleType.typeParameters.isEmpty()) NULLABLE_ANY else of(singleType.typeParameters, false)
                                    return ofArray(elementType)
                                }
                                TokenType.CLASS -> {
                                    if(singleType.typeParameters.isEmpty()) {
                                        return ofClass("")
                                    }
                                    val className =
                                        singleType.typeParameters.first().tokens.entries.first().key.lexeme
                                    return ofClass(className)
                                }
                                TokenType.IDENTIFIER -> {
                                    if(singleType.typeParameters.isNotEmpty()) {
                                        val types = singleType.typeParameters.map { of(listOf(it), false) }
                                        return ofGenericObject(singleToken.key.lexeme, types)
                                    }
                                    return ofObject(singleToken.key.lexeme)
                                }
                                TokenType.TYPE_GENERIC -> {
                                    return Parameter(singleType.typeParameters.first().tokens.keys.first())
                                }
                                else -> {
                                    return Singular(PrimitiveType.get(singleToken.key))
                                }
                            }
                        } else {
                            val types = singleType.tokens.map { of(it.value, false) as Singular }
                            return Union(types)
                        }
                    } else {
                        val types = tokens.map { of(listOf(it), false) as Singular }
                        return Union(types)
                    }
                }
            }
        }

        fun contains(type: Type, inType: Type, sunlite: Sunlite): Boolean {
            currentInterpreter = sunlite
            if(type == UNKNOWN) return false //can't statically determine the type
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
                is SLClosure -> {
                    val function = value.function
                    ofFunction(function.name, function.returnType, function.params)
                }
                is SLBoundMethod -> {
                    val function = value.method.function
                    ofFunction(function.name, function.returnType, function.params)
                }
                is SLUpvalue -> {
                    fromValue(value.closedValue, sunlite)
                }
                is SLNativeFunction ->  ofFunction(value.name, value.returnType, listOf())
                is SLFunction -> ofFunction(value.name, value.returnType, value.params)
                is SLClass -> ofClass(value.name)
                is SLClassInstance -> ofObject(value.clazz.name)
                is SLArray -> ARRAY
                is SLType -> value.value
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