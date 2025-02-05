package sunsetsatellite.lang.lox

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
                if (!javaClass.isAssignableFrom(other.javaClass)) return false
            } else {
                return false
            }

            other as Singular

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

    class Reference(type: PrimitiveType, ref: String, val typeParameters: List<Type> = listOf(), val lox: Lox) : Singular(type, ref) {
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
            val typeHierarchy = lox.typeCollector.typeHierarchy
            if (!typeHierarchy.map { it.type }.contains(other.ref) || !lox.typeCollector.typeHierarchy.map { it.type }.contains(this.ref)) {
                return false
            } else {
                val node: TypeCollector.TypeHierarchyNode = typeHierarchy.find { it.type == other.ref }!!
                node.supertypes ?: return false
                val supertype = node.supertypes.find { it.type == this.ref }
                return if (supertype != null) true else isValidSupertype(node,typeHierarchy)
            }
        }

        private fun isValidSupertype(
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

        fun ofClass(name: String, lox: Lox): Type.Reference {
            return Type.Reference(PrimitiveType.CLASS, name, listOf(), lox)
        }

        fun ofFunction(name: String, lox: Lox): Type.Reference {
            return Type.Reference(PrimitiveType.FUNCTION, name, listOf(), lox)
        }

        fun ofObject(name: String, lox: Lox): Type.Reference {
            return Type.Reference(PrimitiveType.OBJECT, name, listOf(), lox)
        }

        fun of(tokens: List<TypeToken>, lox: Lox): Type {
            if(tokens.size == 1){
                if(tokens[0].pure){
                    return Parameter(tokens[0].token)
                }
                return if(tokens[0].token.type != TokenType.IDENTIFIER){
                    if(tokens[0].token.type == TokenType.CLASS && tokens[0].typeParameters.isNotEmpty()){
                        Reference(PrimitiveType.get(tokens[0].token), tokens[0].token.lexeme, listOf(of(tokens[0].typeParameters,lox)),lox)
                    }
                    Singular(PrimitiveType.get(tokens[0].token))
                } else {
                    if(tokens[0].typeParameters.isNotEmpty()){
                        Reference(PrimitiveType.get(tokens[0].token), tokens[0].token.lexeme, listOf(of(tokens[0].typeParameters,lox)),lox)
                    } else {
                        Reference(PrimitiveType.get(tokens[0].token), tokens[0].token.lexeme, listOf(), lox)
                    }
                }
            } else if (tokens.size > 1) {
                val types: List<Singular> = tokens.map { of(listOf(it),lox) } as List<Singular>
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
                    return inType.types.contains(type) || inType.types.contains(ANY)
                }
            } else {
                return inType.equals(type) || inType == ANY
            }
        }

        fun fromValue(value: Any?, lox: Lox): Type {
            return when (value) {
                is Type -> value
                is Param -> value.type
                is String -> STRING
                is Double -> NUMBER
                is Boolean -> BOOLEAN
                null -> NIL
                is LoxFunction -> ofFunction(value.declaration.name.lexeme, lox)
                is LoxClass -> ofClass(value.name, lox)
                is LoxInterface -> ofClass(value.name, lox)
                is LoxClassInstance -> ofObject(value.name(), lox)
                else -> UNKNOWN
            }
        }

        val UNKNOWN = Singular(PrimitiveType.UNKNOWN)
        val NIL = Singular(PrimitiveType.NIL)
        val ANY = Singular(PrimitiveType.ANY)
        val NULLABLE_ANY = Union(listOf(ANY,NIL))
        val NUMBER = Singular(PrimitiveType.NUMBER)
        val STRING = Singular(PrimitiveType.STRING)
        val BOOLEAN = Singular(PrimitiveType.BOOLEAN)
    }
}