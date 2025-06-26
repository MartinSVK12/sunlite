package sunsetsatellite.lang.sunlite

import kotlin.collections.find

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

    class Reference(type: PrimitiveType, ref: String, val typeParameters: List<Type> = listOf(), val sunlite: Sunlite) : Singular(type, ref) {
        override fun getName(): String {
            return "${type.name.lowercase()}<${ref}${if(typeParameters.isNotEmpty()) "<${typeParameters.joinToString(", ")}>" else ""}>"
        }

        override fun toString(): String {
            return "${type.name.lowercase()}<${ref}${if(typeParameters.isNotEmpty()) "<${typeParameters.joinToString(", ")}>" else ""}>"
        }

        override fun equals(other: Any?): Boolean {
            if(other !is Reference) return false
            if(other.type != type) return false
            if(this.type == other.type && this.ref == other.ref) return true
            /*val typeHierarchy = sunlite.typeCollector.typeHierarchy
            if (!typeHierarchy.map { it.type }.contains(other.ref) || !sunlite.typeCollector.typeHierarchy.map { it.type }.contains(this.ref)) {
                return false
            } else {
                val node: TypeCollector.TypeHierarchyNode = typeHierarchy.find { it.type == other.ref }!!
                node.supertypes ?: return false
                val supertype = node.supertypes.find { it.type == this.ref }
                return if (supertype != null) true else isValidSupertype(node,typeHierarchy)
            }*/
            return false
        }

        /*private fun isValidSupertype(
            node: TypeCollector.TypeHierarchyNode,
            typeHierarchy: MutableList<TypeCollector.TypeHierarchyNode>,
        ): Boolean {
            node.supertypes ?: return false
            node.supertypes.forEach { nextSupertype ->
                val nextNode = typeHierarchy.find { it.type == nextSupertype.type }!!
                nextNode.supertypes ?: return false
                val supertype = nextNode.supertypes.find { it.type == this.ref }
                if (supertype != null) {
                    return true
                }
                return isValidSupertype(nextNode,typeHierarchy)
            }
            return false
        }*/

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

        fun ofClass(name: String, sunlite: Sunlite): Reference {
            return Reference(PrimitiveType.CLASS, name, listOf(), sunlite)
        }

        fun ofFunction(name: String, sunlite: Sunlite): Reference {
            return Reference(PrimitiveType.FUNCTION, name, listOf(), sunlite)
        }

        fun ofObject(name: String, sunlite: Sunlite): Reference {
            return Reference(PrimitiveType.OBJECT, name, listOf(), sunlite)
        }

        fun of(tokens: List<TypeToken>, sunlite: Sunlite): Type {
            if(tokens.size == 1){
                if(tokens[0].pure){
                    return Parameter(tokens[0].token)
                }
                if(tokens[0].token.type != TokenType.IDENTIFIER){
                    if(tokens[0].token.type == TokenType.CLASS && tokens[0].typeParameters.isNotEmpty()){
                        return Reference(PrimitiveType.get(tokens[0].token), tokens[0].token.lexeme, listOf(of(tokens[0].typeParameters,sunlite)),sunlite)
                    }
                    return Singular(PrimitiveType.get(tokens[0].token))
                } else {
                    if(tokens[0].typeParameters.isNotEmpty()){
                        return Reference(PrimitiveType.get(tokens[0].token), tokens[0].token.lexeme, listOf(of(tokens[0].typeParameters,sunlite)),sunlite)
                    } else {
                        return Reference(PrimitiveType.get(tokens[0].token), tokens[0].token.lexeme, listOf(), sunlite)
                    }
                }
            } else if (tokens.size > 1) {
                val types: List<Singular> = tokens.map { of(listOf(it),sunlite) } as List<Singular>
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
                /*is LoxFunction -> ofFunction(value.declaration.name.lexeme, sunlite)
                is LoxClass -> ofClass(value.name, sunlite)
                is LoxInterface -> ofClass(value.name, sunlite)
                is LoxClassInstance -> ofObject(value.name(), sunlite)
                is LoxArray -> ARRAY*/
                else -> UNKNOWN
            }
        }

        val UNKNOWN = Singular(PrimitiveType.UNKNOWN)
        val NIL = Singular(PrimitiveType.NIL)
        val ANY = Singular(PrimitiveType.ANY)
        val CLASS = Singular(PrimitiveType.CLASS)
        val OBJECT = Singular(PrimitiveType.OBJECT)
        val ARRAY = Singular(PrimitiveType.ARRAY)
        val NULLABLE_ANY = Union(listOf(ANY, NIL))
        val NUMBER = Singular(PrimitiveType.NUMBER)
        val STRING = Singular(PrimitiveType.STRING)
        val BOOLEAN = Singular(PrimitiveType.BOOLEAN)
    }
}