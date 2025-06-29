package sunsetsatellite.lang.sunlite

import com.sun.org.apache.xpath.internal.operations.Bool

abstract class Expr: Element {
	data class Binary(val left: Expr, val operator: Token, val right: Expr) : Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitBinaryExpr(this)
		}

		override fun getLine(): Int {
			return operator.line
		}

		override fun getFile(): String? {
			return operator.file
		}

		override fun getExprType(): Type {
            if (operator.type == TokenType.STAR || operator.type == TokenType.SLASH || operator.type == TokenType.MINUS) {
                return Type.NUMBER
            }
			if(operator.type != TokenType.PLUS) {
				return Type.BOOLEAN
			} else {
				if(left.getExprType() == Type.NUMBER && right.getExprType() == Type.NUMBER) {
					return Type.NUMBER
				} else {
					return Type.STRING
				}
			}
        }
	}

	data class Grouping(val expression: Expr) : Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitGroupingExpr(this)
		}

		override fun getLine(): Int {
			return expression.getLine()
		}

		override fun getFile(): String? {
			return expression.getFile()
		}

		override fun getExprType(): Type {
			return expression.getExprType()
		}
	}

	data class Unary(val operator: Token, val right: Expr) : Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitUnaryExpr(this)
		}

		override fun getLine(): Int {
			return operator.line
		}

		override fun getFile(): String? {
			return operator.file
		}

		override fun getExprType(): Type {
			if(operator.type == TokenType.BANG) {
				return Type.BOOLEAN
			} else if(operator.type == TokenType.MINUS) {
				return Type.NUMBER
			}
			return Type.UNKNOWN
		}
	}

	data class Literal(val value: Any?, val lineNumber: Int, val currentFile: String?, val type: Type = Type.UNKNOWN): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitLiteralExpr(this)
		}

		override fun getLine(): Int {
			return lineNumber
		}

		override fun getFile(): String? {
			return currentFile
		}

		override fun getExprType(): Type {
			return type
		}
	}

	data class Variable(val name: Token, val type: Type = Type.UNKNOWN, val constant: Boolean = false): Expr(), NamedExpr {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitVariableExpr(this)
		}

		override fun getLine(): Int {
			return name.line
		}

		override fun getFile(): String? {
			return name.file
		}

		override fun getNameToken(): Token {
			return name
		}

		override fun getExprType(): Type {
			return type
		}
	}

	/*class GenericVariable(name: Token, val typeParameters: List<Param>): Variable(name) {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitVariableExpr(this)
		}

		override fun getLine(): Int {
			return name.line
		}

		override fun getFile(): String? {
			return name.file
		}

		override fun getNameToken(): Token {
			return name
		}
	}*/

	data class Assign(val name: Token, val value: Expr, val operator: TokenType, val type: Type): Expr(), NamedExpr {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitAssignExpr(this)
		}

		override fun getLine(): Int {
			return name.line
		}

		override fun getFile(): String? {
			return name.file
		}

		override fun getNameToken(): Token {
			return name
		}

		override fun getExprType(): Type {
			return type
		}

	}

	data class Logical(val left: Expr, val operator: Token, val right: Expr): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitLogicalExpr(this)
		}

		override fun getLine(): Int {
			return operator.line
		}

		override fun getFile(): String? {
			return operator.file
		}

		override fun getExprType(): Type {
			return Type.BOOLEAN
		}
	}

	data class Call(val callee: Expr, val paren: Token, val arguments: List<Expr>, val typeParams: List<Type>): Expr(), GenericExpr {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitCallExpr(this)
		}

		override fun getLine(): Int {
			return paren.line
		}

		override fun getFile(): String? {
			return paren.file
		}

		override fun getExprType(): Type {
			val type = callee.getExprType()
			if(type is Type.Reference){
				if(type.type == PrimitiveType.CLASS){
					if(!typeParams.isEmpty()){
						val rawType = type.returnType
						return Type.ofGenericObject(rawType.getName(), typeParams)
					}
				}
				return type.returnType
			}
			return Type.UNKNOWN
        }

		override fun getTypeArguments(): List<Type> {
			return typeParams
		}
	}

	data class Lambda(val function: Stmt.Function): Expr(), NamedExpr {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitLambdaExpr(this)
		}

		override fun getLine(): Int {
			return function.name.line
		}

		override fun getFile(): String? {
			return function.getFile()
		}

		override fun getNameToken(): Token {
			return function.name
		}

		override fun getExprType(): Type {
			return Type.ofFunction(function.name.lexeme, function.returnType, function.params)
		}
	}

	data class Get(val obj: Expr, val name: Token, val type: Type = Type.UNKNOWN, val constant: Boolean = false,
				   var typeParams: List<Type> =
					   if(obj.getExprType() is Type.Reference) {
						   val ref = obj.getExprType() as Type.Reference
						   if(ref.type == PrimitiveType.OBJECT){
							   ref.typeParams
						   } else {
							   listOf()
						   }
						} else {
							listOf()
					   })
		: Expr(), NamedExpr, GenericExpr {


		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitGetExpr(this)
		}

		override fun getLine(): Int {
			return name.line
		}

		override fun getFile(): String? {
			return name.file
		}

		override fun getNameToken(): Token {
			return name
		}

		override fun getExprType(): Type {
			return type
		}

		override fun getTypeArguments(): List<Type> {
			return typeParams
		}
	}

	data class ArrayGet(val obj: Expr, val what: Expr, val token: Token, val type: Type = Type.UNKNOWN): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitArrayGetExpr(this)
		}

		override fun getLine(): Int {
			return token.line
		}

		override fun getFile(): String? {
			return token.file
		}

		override fun getExprType(): Type {
			//todo
			return Type.NULLABLE_ANY
		}
	}

	data class Set(val obj: Expr, val name: Token, val value: Expr, val operator: TokenType, val type: Type = Type.UNKNOWN): Expr(), NamedExpr {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitSetExpr(this)
		}

		override fun getLine(): Int {
			return name.line
		}

		override fun getFile(): String? {
			return name.file
		}

		override fun getNameToken(): Token {
			return name
		}

		override fun getExprType(): Type {
			return type
		}

	}

	data class ArraySet(val obj: Expr, val what: Expr, val value: Expr, val token: Token, val operator: TokenType, val type: Type = Type.UNKNOWN): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitArraySetExpr(this)
		}

		override fun getLine(): Int {
			return token.line
		}

		override fun getFile(): String? {
			return token.file
		}

		override fun getExprType(): Type {
			return type
		}
	}

	data class This(val keyword: Token, val type: Type = Type.UNKNOWN): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitThisExpr(this)
		}

		override fun getLine(): Int {
			return keyword.line
		}

		override fun getFile(): String? {
			return keyword.file
		}

		override fun getExprType(): Type {
			return type
		}
	}

	data class Super(val keyword: Token, val method: Token, val type: Type = Type.UNKNOWN): Expr(), NamedExpr {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitSuperExpr(this)
		}

		override fun getLine(): Int {
			return keyword.line
		}

		override fun getFile(): String? {
			return keyword.file
		}

		override fun getNameToken(): Token {
			return method
		}

		override fun getExprType(): Type {
			return type
		}
	}

	data class Check(val left: Expr, val operator: Token, val right: Type): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitCheckExpr(this)
		}

		override fun getLine(): Int {
			return operator.line
		}

		override fun getFile(): String? {
			return operator.file
		}

		override fun getExprType(): Type {
			return Type.BOOLEAN
		}
	}

	data class Cast(val left: Expr, val operator: Token, val right: Type): Expr() {
		override fun <R> accept(visitor: Visitor<R>): R? {
			return visitor.visitCastExpr(this)
		}

		override fun getLine(): Int {
			return operator.line
		}

		override fun getFile(): String? {
			return operator.file
		}

		override fun getExprType(): Type {
			return right
		}
	}

	interface Visitor<R> {
		fun visitBinaryExpr(expr: Binary): R
		fun visitGroupingExpr(expr: Grouping): R
		fun visitUnaryExpr(expr: Unary): R
		fun visitLiteralExpr(expr: Literal): R
		fun visitVariableExpr(expr: Variable): R
		fun visitAssignExpr(expr: Assign): R
		fun visitLogicalExpr(expr: Logical): R
		fun visitCallExpr(expr: Call): R?
		fun visitLambdaExpr(expr: Lambda): R
		fun visitGetExpr(expr: Get): R
		fun visitArrayGetExpr(expr: ArrayGet): R
		fun visitArraySetExpr(expr: ArraySet): R
		fun visitSetExpr(expr: Set): R
		fun visitThisExpr(expr: This): R
		fun visitSuperExpr(expr: Super): R
		fun visitCheckExpr(expr: Check): R
		fun visitCastExpr(expr: Cast): R
	}

	interface NamedExpr: Element {
		fun getNameToken(): Token
	}

	interface GenericExpr: Element {
		fun getTypeArguments(): List<Type>
	}

	abstract fun <R> accept(visitor: Visitor<R>): R?
	abstract fun getExprType(): Type
}