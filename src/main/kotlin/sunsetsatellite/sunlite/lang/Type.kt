package sunsetsatellite.sunlite.lang

import sunsetsatellite.sunlite.vm.*

abstract class Type {

    open class Singular(val type: PrimitiveType, val ref: String = "") : Type() {

        override fun getName(): String {
            return type.name.lowercase()
        }

        override fun getDescriptor(): String {
            return type.descriptor.toString()
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

            if (type == PrimitiveType.FLOAT || type == PrimitiveType.DOUBLE) {
                if (other.type != PrimitiveType.FLOAT && other.type != PrimitiveType.DOUBLE && numericOrder.containsKey(
                        other.type
                    )
                ) {
                    return true
                }
            }

            if (type == PrimitiveType.FLOAT && other.type == PrimitiveType.DOUBLE) {
                return false
            } else if (type == PrimitiveType.DOUBLE && other.type == PrimitiveType.FLOAT) {
                return true
            }

            if (numericOrder.containsKey(type) && numericOrder.containsKey(other.type)) {
                return numericOrder[other.type]!! <= numericOrder[type]!!
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

    class Reference(
        type: PrimitiveType,
        ref: String,
        val returnType: Type,
        val params: List<Param> = listOf(),
        val typeParams: List<Param> = listOf()
    ) : Singular(type, ref) {

        override fun getDescriptor(): String {
            if(type.isReference){
                when (type) {
                    PrimitiveType.OBJECT -> {
                        return PrimitiveType.OBJECT.descriptor+ref+";"
                    }
                    PrimitiveType.CLASS -> {
                        return PrimitiveType.CLASS.descriptor+ref+";"
                    }
                    PrimitiveType.ARRAY -> {
                        return PrimitiveType.ARRAY.descriptor+returnType.getDescriptor()+";"
                    }
                    PrimitiveType.TABLE -> {
                        return PrimitiveType.TABLE.descriptor+typeParams[0].type.getDescriptor()+"="+returnType.getDescriptor()+";"
                    }
                    PrimitiveType.FUNCTION -> {
                        return PrimitiveType.FUNCTION.descriptor + params.joinToString("") { it.type.getDescriptor() } +")"+returnType.getDescriptor()+";"
                    }
                    else -> super.getDescriptor()
                }
            }
            return super.getDescriptor()
        }

        override fun getName(): String {
            return ref
        }

        override fun toString(): String {
            when (type) {
                PrimitiveType.FUNCTION -> {
                    return "${type.name.lowercase()}${if (typeParams.isNotEmpty()) "<${typeParams.joinToString()}>" else ""} '${ref}(${
                        params.map { "${it.token.lexeme}: ${it.type}" }.joinToString()
                    }): ${returnType}'"
                }

                PrimitiveType.ARRAY -> {
                    return "${type.name.lowercase()} '${returnType}'"
                }

                PrimitiveType.TABLE -> {
                    return "${type.name.lowercase()} '${typeParams[0].type} -> ${returnType}'"
                }

                PrimitiveType.OBJECT -> {
                    return "${type.name.lowercase()}${if (typeParams.isNotEmpty()) "<${typeParams.joinToString()}>" else ""}${if (ref != "") " '${ref}'" else ""}"
                }

                else -> {
                    return "${type.name.lowercase()}${if (ref != "") " '${ref}'" else ""}"
                }
            }
        }

        override fun equals(other: Any?): Boolean {
            if (other !is Reference) return false
            if (other.type != type) return false
            when (this.type) {
                PrimitiveType.FUNCTION -> {
                    if (this.returnType == other.returnType) {
                        if (this.params.size != other.params.size) return false
                        val types = params.map { it.type }
                        val otherTypes = other.params.map { it.type }
                        return !types.zip(otherTypes)
                            .any { !contains(it.second, it.first, currentInterpreter!!) }
                    }
                }

                PrimitiveType.OBJECT -> {
                    if (currentInterpreter == null) return false
                    if (ref == other.ref) return true
                    return traverseTypeHierarchy(other.ref)
                }

                PrimitiveType.ARRAY -> {
                    return contains(returnType, other.returnType, currentInterpreter!!)
                }

                PrimitiveType.TABLE -> {
                    if (!contains(typeParams[0].type, other.typeParams[0].type, currentInterpreter!!)) return false
                    if (!contains(returnType, other.returnType, currentInterpreter!!)) return false
                    return true
                }

                PrimitiveType.CLASS -> {
                    if (this.ref == "") return true
                    if (this.ref == other.ref) return true
                }

                else -> {
                    return true
                }
            }
            return false
        }

        private fun traverseTypeHierarchy(other: String): Boolean {
            val parents = currentInterpreter?.collector?.typeHierarchy[other]
            val superclass = parents?.superclass
            val interfaces = parents?.superinterfaces
            if (superclass == this.ref) return true
            if (interfaces != null && interfaces.contains(this.ref)) return true
            if ((superclass == null || superclass == "<nil>" || superclass == "") && (interfaces == null || interfaces.isEmpty())) return false
            if (superclass != null) {
                val success = traverseTypeHierarchy(superclass)
                if (success) return true
            }
            if (interfaces != null) {
                for (int in interfaces) {
                    val intSuccess = traverseTypeHierarchy(int)
                    if (intSuccess) return true
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

        override fun getDescriptor(): String {
            return types.joinToString("|") { it.getDescriptor() }
        }

        override fun toString(): String {
            return getName()
        }
    }

    class Parameter(val name: Token) : Singular(PrimitiveType.GENERIC, name.lexeme) {
        override fun getName(): String {
            return name.lexeme
        }

        override fun getDescriptor(): String {
            return PrimitiveType.GENERIC.descriptor.toString()
        }

        override fun toString(): String {
            return "generic '${name.lexeme}'"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Parameter) return false

            //if (name.lexeme != other.name.lexeme) return false

            return true
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }


    }

    abstract fun getName(): String
    abstract fun getDescriptor(): String

    companion object {

        private val numericOrder: MutableMap<PrimitiveType, Int> = mutableMapOf(
            PrimitiveType.BYTE to 0, PrimitiveType.SHORT to 1, PrimitiveType.INT to 2, PrimitiveType.LONG to 3
        )

        var currentInterpreter: Sunlite? = null

        fun ofClass(name: String, params: List<Param> = listOf()): Reference {
            return Reference(PrimitiveType.CLASS, name, ofObject(name), listOf(), params)
        }

        fun ofArray(elementType: Type): Reference {
            return Reference(PrimitiveType.ARRAY, "<array>", elementType, listOf())
        }

        fun ofTable(keyType: Type, valueType: Type): Reference {
            return Reference(
                PrimitiveType.TABLE,
                "<table>",
                valueType,
                listOf(),
                listOf(Param(Token.identifier("<key>"), keyType))
            )
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

        fun ofGenericObject(name: String, typeParams: List<Param>): Reference {
            val reference = let {
                val reference = Reference(PrimitiveType.OBJECT, name, OBJECT, listOf(), typeParams)
                Reference(PrimitiveType.OBJECT, name, reference, listOf(), typeParams)
            }

            return reference
        }

        fun ofGenericFunction(name: String, returnType: Type, params: List<Param>, typeParams: List<Param>): Reference {
            return Reference(PrimitiveType.FUNCTION, name, returnType, params, typeParams)
        }

        fun of(tokens: List<TypeToken>, sunlite: Sunlite, topmost: Boolean = true): Type {
            if (tokens.isEmpty()) {
                throw IllegalArgumentException("No types provided.")
            } else if (tokens.size > 1 && topmost) {
                throw IllegalArgumentException("There can be only one topmost type.")
            } else {
                if (topmost) {
                    val topmostType = tokens.first()
                    if (topmostType.tokens.size == 1) {
                        val singleToken = topmostType.tokens.entries.first()
                        when (singleToken.key.type) {
                            TokenType.TYPE_FUNCTION -> {
                                val returnType = if (topmostType.typeParameters.isEmpty()) NIL else of(
                                    listOf(topmostType.typeParameters.last()),
                                    sunlite,
                                    false
                                )
                                val paramTokens = topmostType.typeParameters.dropLast(1)
                                val params =
                                    paramTokens.map { Param(Token.identifier(""), of(listOf(it), sunlite, false)) }
                                return ofFunction("", returnType, params)
                            }

                            TokenType.TYPE_ARRAY -> {
                                val elementType = if (topmostType.typeParameters.isEmpty()) NULLABLE_ANY else of(
                                    topmostType.typeParameters,
                                    sunlite,
                                    false
                                )
                                return ofArray(elementType)
                            }

                            TokenType.TYPE_TABLE -> {
                                val keyType = if (topmostType.typeParameters.isEmpty()) NULLABLE_ANY else of(
                                    listOf(
                                        topmostType.typeParameters[0]
                                    ), sunlite, false
                                )
                                val valueType = if (topmostType.typeParameters.isEmpty()) NULLABLE_ANY else of(
                                    listOf(
                                        topmostType.typeParameters[1]
                                    ), sunlite, false
                                )
                                return ofTable(keyType, valueType)
                            }

                            TokenType.TYPE_CLASS -> {
                                if (topmostType.typeParameters.isEmpty()) {
                                    return ofClass("")
                                }
                                val className =
                                    topmostType.typeParameters.first().tokens.entries.first().key.lexeme
                                return ofClass(className)
                            }

                            TokenType.IDENTIFIER -> {
                                if (topmostType.typeParameters.isNotEmpty()) {
                                    val types = topmostType.typeParameters.map { of(listOf(it), sunlite, false) }
                                    val name = singleToken.key.lexeme
                                    val typeParams = sunlite.collector?.typeHierarchy[name]?.typeParameters
                                    return ofGenericObject(
                                        singleToken.key.lexeme,
                                        types.mapIndexed { i, it ->
                                            Param(
                                                Token.identifier(typeParams?.get(i) ?: "?"),
                                                it
                                            )
                                        })
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
                        val types = topmostType.tokens.values.map { of(it, sunlite, false) as Singular }
                        return Union(types)
                    }
                } else {
                    if (tokens.size == 1) {
                        val singleType = tokens.first()
                        if (singleType.tokens.size == 1) {
                            val singleToken = singleType.tokens.entries.first()
                            when (singleToken.key.type) {
                                TokenType.TYPE_FUNCTION -> {
                                    val returnType = if (singleType.typeParameters.isEmpty()) NIL else of(
                                        listOf(singleType.typeParameters.last()),
                                        sunlite,
                                        false
                                    )
                                    val paramTokens = singleType.typeParameters.dropLast(1)
                                    val params =
                                        paramTokens.map { Param(Token.identifier(""), of(listOf(it), sunlite, false)) }
                                    return ofFunction("", returnType, params)
                                }

                                TokenType.TYPE_ARRAY -> {
                                    val elementType = if (singleType.typeParameters.isEmpty()) NULLABLE_ANY else of(
                                        singleType.typeParameters,
                                        sunlite,
                                        false
                                    )
                                    return ofArray(elementType)
                                }

                                TokenType.TYPE_TABLE -> {
                                    val keyType = if (singleType.typeParameters.isEmpty()) NULLABLE_ANY else of(
                                        listOf(
                                            singleType.typeParameters[0]
                                        ), sunlite, false
                                    )
                                    val valueType = if (singleType.typeParameters.isEmpty()) NULLABLE_ANY else of(
                                        listOf(
                                            singleType.typeParameters[1]
                                        ), sunlite, false
                                    )
                                    return ofTable(keyType, valueType)
                                }

                                TokenType.TYPE_CLASS -> {
                                    if (singleType.typeParameters.isEmpty()) {
                                        return ofClass("")
                                    }
                                    val className =
                                        singleType.typeParameters.first().tokens.entries.first().key.lexeme
                                    return ofClass(className)
                                }

                                TokenType.IDENTIFIER -> {
                                    if (singleType.typeParameters.isNotEmpty()) {
                                        val types = singleType.typeParameters.map { of(listOf(it), sunlite, false) }
                                        val name = singleToken.key.lexeme
                                        val typeParams = sunlite.collector?.typeHierarchy[name]?.typeParameters
                                        return ofGenericObject(
                                            singleToken.key.lexeme,
                                            types.mapIndexed { i, it ->
                                                Param(
                                                    Token.identifier(
                                                        typeParams?.get(i) ?: "??"
                                                    ), it
                                                )
                                            })
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
                            val types = singleType.tokens.map { of(it.value, sunlite, false) as Singular }
                            return Union(types)
                        }
                    } else {
                        val types = tokens.map { of(listOf(it), sunlite, false) as Singular }
                        return Union(types)
                    }
                }
            }
        }

        fun contains(type: Type, inType: Type, sunlite: Sunlite): Boolean {
            currentInterpreter = sunlite
            if (type == UNKNOWN) return false //can't statically determine the type
            if (inType is Union) {
                if (type is Union) {
                    return inType.types.containsAll(type.types) || inType.types.contains(ANY)
                } else {
                    return inType.types.any { if(it is Parameter) true else it.equals(type) } || inType.types.contains(ANY)
                }
            } else {
                if (type is Union) {
                    return type.types.contains(ANY)
                } else {
                    return inType.equals(type) || inType == ANY
                }
            }
        }

        fun fromValue(value: Any?, sunlite: Sunlite): Type {
            return when (value) {
                is Type -> value
                is Param -> value.type
                is String -> STRING
                is Byte -> BYTE
                is Short -> SHORT
                is Int -> INT
                is Long -> LONG
                is Float -> FLOAT
                is Double -> DOUBLE //NUMBER
                is Boolean -> BOOLEAN
                is Unit -> NIL
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

                is SLNativeFunction -> ofFunction(value.name, value.returnType, listOf())
                is SLFunction -> ofFunction(value.name, value.returnType, value.params)
                is SLClass -> ofClass(value.name)
                is SLClassInstance -> ofObject(value.clazz.name)
                is SLArray -> {
                    ofArray(value.type)
                }
                is SLTable -> ofTable(value.types.first, value.types.second)
                is SLType -> value.value
                else -> UNKNOWN
            }
        }

        fun reify(type: Type, typeArgs: List<Param>): Type {

            if(type is Reference){
                val o = object  {
                    val primitive = type.type
                    var returnType = type.returnType
                    val ref = type.ref
                    var typeParams = type.typeParams
                    var params = type.params
                }

                o.returnType = reify(o.returnType, typeArgs)
                o.params = o.params.map { Param(it.token, reify(it.type, typeArgs)) }
                o.typeParams = o.typeParams.map { Param(it.token, reify(it.type, typeArgs)) }

                return Reference(o.primitive, o.ref, o.returnType, o.params, o.typeParams)
            }

            if(type is Parameter){
                typeArgs.firstOrNull { it.token.lexeme == type.name.lexeme }?.let {
                    return it.type
                }
            }

            return type
        }

        val UNKNOWN = Singular(PrimitiveType.UNKNOWN)
        val NIL = Singular(PrimitiveType.NIL)
        val ANY = Singular(PrimitiveType.ANY)
        val CLASS = Singular(PrimitiveType.CLASS)
        val FUNCTION = Singular(PrimitiveType.FUNCTION)
        val OBJECT = Singular(PrimitiveType.OBJECT)
        val ARRAY = Singular(PrimitiveType.ARRAY)
        val TABLE = Singular(PrimitiveType.TABLE)
        val NULLABLE_ANY = Union(listOf(ANY, NIL))
        val BYTE = Singular(PrimitiveType.BYTE)
        val SHORT = Singular(PrimitiveType.SHORT)
        val INT = Singular(PrimitiveType.INT)
        val LONG = Singular(PrimitiveType.LONG)
        val FLOAT = Singular(PrimitiveType.FLOAT)
        val DOUBLE = Singular(PrimitiveType.DOUBLE)

        //val NUMBER = Singular(PrimitiveType.NUMBER)
        val STRING = Singular(PrimitiveType.STRING)
        val BOOLEAN = Singular(PrimitiveType.BOOLEAN)
    }
}