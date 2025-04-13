package sunsetsatellite.interpreter.sunlite

import sunsetsatellite.lang.sunlite.*
import java.util.*

class TypeResolver(val collector: TypeCollector, val sunlite: Sunlite) : Stmt.Visitor<Type>, Expr.Visitor<Type> {

    private var typeCheckStack: Stack<CheckType> = Stack<CheckType>().let { it.push(CheckType.NORMAL); return@let it }

    var currentParent: Stmt.NamedStmt? = null

    var resolvePrevious = false

    enum class CheckType {
        NORMAL,
        RETURN,
    }

    fun resolve(stmt: Stmt, parent: Stmt.NamedStmt?): Type {
        try {
            currentParent = parent
            val type = resolve(stmt)
            currentParent = null
            return type
        } catch (e: NoSuchElementException) {
            return Type.UNKNOWN
        }

    }

    fun resolve(expr: Expr, parent: Stmt.NamedStmt?, switch: Boolean = false): Type {
        try {
            if(switch) resolvePrevious = true
            currentParent = parent
            val type = resolve(expr)
            currentParent = null
            resolvePrevious = false
            return type
        } catch (e: NoSuchElementException) {
            return Type.UNKNOWN
        }
    }

    private fun resolve(stmt: Stmt, checkType: CheckType = CheckType.NORMAL): Type {
        typeCheckStack.push(checkType)
        val type: Type
        try {
            type = stmt.accept(this)
        } catch (e: NoSuchElementException) {
            return Type.UNKNOWN
        }
        typeCheckStack.pop()
        return type
    }

    private fun resolve(expr: Expr, checkType: CheckType = CheckType.NORMAL): Type {
        typeCheckStack.push(checkType)
        val type: Type
        try {
            type = expr.accept(this)!!
        } catch (e: NoSuchElementException) {
            return Type.UNKNOWN
        }
        typeCheckStack.pop()
        return type
    }

    override fun visitBinaryExpr(expr: Expr.Binary): Type {
        val leftType = resolve(expr.left)
        val rightType = resolve(expr.right)
        return if(leftType == Type.NUMBER && rightType == Type.NUMBER) {
            Type.NUMBER
        } else {
            Type.STRING
        }
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Type {
        return resolve(expr.expression)
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Type {
        return resolve(expr.right)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Type {
        return when (expr.value) {
            is String -> Type.STRING
            is Double -> Type.NUMBER
            is Boolean -> Type.BOOLEAN
            null -> Type.NIL
            else -> Type.UNKNOWN
        }
    }

    override fun visitVariableExpr(expr: Expr.Variable): Type {
        var info: TypeCollector.TypeInfo? = collector.info
            .filter { it.parent?.first == currentParent?.getNameToken()?.lexeme }
            .firstOrNull { it.self.first == expr.name.lexeme }
        if(info == null) {
            info = collector.info
                .filter { it.parent == null }
                .firstOrNull { it.self.first == expr.name.lexeme }
        }
        return when (typeCheckStack.peek()){
            CheckType.NORMAL -> info?.type ?: Type.UNKNOWN
            CheckType.RETURN -> info?.returnType ?: Type.UNKNOWN
            null -> Type.UNKNOWN
        }
    }

    override fun visitAssignExpr(expr: Expr.Assign): Type {
        return resolve(expr.value)
    }

    override fun visitLogicalExpr(expr: Expr.Logical): Type {
        return Type.BOOLEAN
    }

    override fun visitCallExpr(expr: Expr.Call): Type {
        return resolve(expr.callee, CheckType.RETURN)
    }

    override fun visitLambdaExpr(expr: Expr.Lambda): Type {
        return Type.Reference(PrimitiveType.FUNCTION, expr.function.name.lexeme, listOf(), sunlite)
    }

    override fun visitGetExpr(expr: Expr.Get): Type {
        if(expr.obj is Expr.NamedExpr){
            val objType = resolve(expr.obj)
            if(objType == Type.UNKNOWN) return Type.UNKNOWN
            if(resolvePrevious) return objType
            val info: TypeCollector.TypeInfo? = collector.info
                .filter { it.parent?.first == (objType as Type.Reference).ref }
                .firstOrNull { it.self.first == expr.name.lexeme }
            if(typeCheckStack.peek() == CheckType.RETURN){
                if(info != null && info.returnType is Type.Parameter){
                    if(objType is Type.Reference){
                        val calleeInfo = collector.info.filter { it.parent == currentParent }.first { it.self.first == objType.ref }
                        val typeParameters = calleeInfo.typeParameters
                        var typeParamIndex = -1
                        typeParameters.forEachIndexed { i, typeParam -> if(typeParam.token.lexeme == info.returnType.name.lexeme) typeParamIndex = i }
                        if(typeParamIndex > -1 && objType.typeParameters.size > typeParamIndex){
                            return objType.typeParameters[typeParamIndex]
                        } else {
                            sunlite.error(expr.name,"Unspecified type parameter ${info.returnType}.")
                        }
                    }
                }
            }
            return when (typeCheckStack.peek()){
                CheckType.NORMAL -> info?.type ?: Type.UNKNOWN
                CheckType.RETURN -> info?.returnType ?: Type.UNKNOWN
                null -> Type.UNKNOWN
            }
        }

        if(expr.obj is Expr.This) {
            if(currentParent is Stmt.Function) {
                val selfInfo: TypeCollector.TypeInfo? = collector.info.firstOrNull{ it.self.second == currentParent }
                selfInfo ?: return Type.UNKNOWN
                val parentInfo = collector.info.filter { it.parent?.first == selfInfo.parent?.first }.firstOrNull { it.self.first == expr.name.lexeme }
                parentInfo ?: return Type.UNKNOWN
                return when (typeCheckStack.peek()){
                    CheckType.NORMAL -> parentInfo.type
                    CheckType.RETURN -> parentInfo.returnType
                    null -> Type.UNKNOWN
                }
            }
        }

        return resolve(expr.obj)
    }

    override fun visitDynamicGetExpr(expr: Expr.DynamicGet): Type {
        return Type.NULLABLE_ANY
    }

    override fun visitDynamicSetExpr(expr: Expr.DynamicSet): Type {
        return Type.NULLABLE_ANY
    }

    override fun visitSetExpr(expr: Expr.Set): Type {
        return resolve(expr.value)
    }

    override fun visitThisExpr(expr: Expr.This): Type {
        return Type.ANY
    }

    override fun visitSuperExpr(expr: Expr.Super): Type {
        return Type.ANY
    }

    override fun visitCheckExpr(expr: Expr.Check): Type {
        return Type.BOOLEAN
    }

    override fun visitCastExpr(expr: Expr.Cast): Type {
        return expr.right
    }

    override fun visitExprStmt(stmt: Stmt.Expression): Type {
        return resolve(stmt.expr)
    }

    override fun visitPrintStmt(stmt: Stmt.Print): Type {
        return Type.UNKNOWN
    }

    override fun visitVarStmt(stmt: Stmt.Var): Type {
        return stmt.type
    }

    override fun visitBlockStmt(stmt: Stmt.Block): Type {
        return Type.UNKNOWN
    }

    override fun visitIfStmt(stmt: Stmt.If): Type {
        return Type.UNKNOWN
    }

    override fun visitWhileStmt(stmt: Stmt.While): Type {
        return Type.UNKNOWN
    }

    override fun visitBreakStmt(stmt: Stmt.Break): Type {
        return Type.UNKNOWN
    }

    override fun visitContinueStmt(stmt: Stmt.Continue): Type {
        return Type.UNKNOWN
    }

    override fun visitFunctionStmt(stmt: Stmt.Function): Type {
        return Type.UNKNOWN
    }

    override fun visitReturnStmt(stmt: Stmt.Return): Type {
        return Type.UNKNOWN
    }

    override fun visitClassStmt(stmt: Stmt.Class): Type {
        return Type.UNKNOWN
    }

    override fun visitInterfaceStmt(stmt: Stmt.Interface): Type {
        return Type.UNKNOWN
    }

    override fun visitImportStmt(stmt: Stmt.Import): Type {
        return Type.UNKNOWN
    }
}