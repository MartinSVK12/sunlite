package sunsetsatellite.vm.sunlite

import sunsetsatellite.lang.sunlite.TokenType.*
import sunsetsatellite.lang.sunlite.*


class Compiler(val sunlite: Sunlite, val vm: VM, val enclosing: Compiler?): Expr.Visitor<Unit>, Stmt.Visitor<Unit> {

	class Local(val name: Token, var depth: Int, var isCaptured: Boolean = false)

	class Upvalue(val index: Int, val isLocal: Boolean)

	var currentClass: ClassCompiler? = null

	var currentFile: String? = null
	var currentFunctionType: FunctionType = FunctionType.FUNCTION
	val chunk = MutableChunk()
	val locals: MutableList<Local> = mutableListOf()
	val upvalues: MutableList<Upvalue> = mutableListOf()
	var localScopeDepth: Int = 0
	var topLevel: Boolean = false

	val incompleteBreaks: MutableList<Int> = mutableListOf()
	val incompleteContinues: MutableList<Int> = mutableListOf()

	fun compile(type: FunctionType, statements: List<Stmt>, path: String? = null, name: String = "", arity: Int = 0): SLFunction {
		currentFile = path
		chunk.debugInfo.file = currentFile
		currentFunctionType = type

		if(type == FunctionType.METHOD || type == FunctionType.INITIALIZER) {
			addIdentifier("this",Expr.This(Token.identifier("this")) )
			locals.add(0,Local(Token.identifier("this", 0, currentFile), 0))
		}

		for (statement in statements) {
			compile(statement)
		}
		if(name == ""){
			topLevel = true
		} else {
			chunk.debugInfo.name = name
		}

		if(type == FunctionType.INITIALIZER){
			emitByte(Opcodes.GET_LOCAL, statements.lastOrNull())
			emitShort(0, statements.lastOrNull())
			emitByte(Opcodes.RETURN, statements.lastOrNull())
		} else {
			emitByte(Opcodes.NIL,statements.lastOrNull());
			emitByte(Opcodes.RETURN,statements.lastOrNull())
		}

		if(Sunlite.debug){
			sunlite.printInfo(Disassembler.disassembleChunk(chunk.toImmutable()))
		}

		incompleteBreaks.forEach {
			sunlite.error(chunk.debugInfo.lines[it], "Unexpected 'break' outside of loop.", chunk.debugInfo.file)
		}
		incompleteContinues.forEach {
			sunlite.error(chunk.debugInfo.lines[it], "Unexpected 'continue' outside of loop.", chunk.debugInfo.file)
		}

		return SLFunction(name, chunk.toImmutable(), arity, upvalues.size)
	}

	private fun compile(stmt: Stmt) {
		stmt.accept(this)
	}

	private fun compile(expr: Expr) {
		expr.accept(this)
	}

	private fun emitByte(byte: Int, expr: Element) {
		chunk.code.add(byte.toByte())
		chunk.debugInfo.lines.add(expr.getLine())
	}

	private fun emitBytes(byte: Int, byte2: Int, expr: Element) {
		emitByte(byte, expr)
		emitByte(byte2, expr)
	}


	private fun emitByte(byte: Opcodes, expr: Element?) {
		chunk.code.add(byte.ordinal.toByte())
		chunk.debugInfo.lines.add(expr?.getLine() ?: 0)
	}

	private fun emitShort(short: Int, expr: Element?) {
		chunk.code.add(((short ushr 8) and 0xFF).toByte())
		chunk.code.add((short and 0xFF).toByte())
		chunk.debugInfo.lines.add(expr?.getLine() ?: 0)
		chunk.debugInfo.lines.add(expr?.getLine() ?: 0)
	}

	private fun emitBytes(byte: Opcodes, byte2: Opcodes, expr: Element) {
		emitByte(byte, expr)
		emitByte(byte2, expr)
	}

	private fun emitBytes(byte: Int, byte2: Opcodes, expr: Element) {
		emitByte(byte, expr)
		emitByte(byte2, expr)
	}

	private fun emitBytes(byte: Opcodes, byte2: Int, expr: Element) {
		emitByte(byte, expr)
		emitByte(byte2, expr)
	}

	private fun emitConstant(value: AnySunliteValue, expr: Element) {
		emitByte(Opcodes.CONSTANT, expr)
		emitShort(addConstant(value, expr), expr)
	}

	private fun emitLoop(loopStart: Int, e: Element) {
		emitByte(Opcodes.LOOP, e)
		val offset = chunk.size() - loopStart + 2
		if(offset > Short.MAX_VALUE){
			sunlite.error(e.getLine(),"Loop body too large.")
		}

		emitByte((offset shr 8) and 0xff, e)
		emitByte(offset and 0xff, e)
	}

	private fun emitJump(byte: Opcodes, e: Element): Int {
		emitByte(byte, e)
		emitByte(0xFF,e)
		emitByte(0xFF,e)
		return chunk.size() - 2
	}

	private fun patchJump(offset: Int, e: Element) {
		val jump = chunk.size() - offset - 2

		if(jump > Short.MAX_VALUE) {
			sunlite.error(e.getLine(), "Too much code to jump over.")
		}

		chunk.code[offset] = ((jump ushr 8) and 0xFF).toByte()
		chunk.code[offset + 1] = (jump and 0xFF).toByte()
	}

	private fun addConstant(value: AnySunliteValue, e: Element): Int {
		if(chunk.constants.contains(value)) {
			return chunk.constants.indexOf(value)
		}
		chunk.constants.add(value)
		val index: Int = chunk.constants.size - 1
		if(index > Short.MAX_VALUE) {
			sunlite.error(e.getLine(), "Too many constants in one chunk.")
			return 0
		}
		return index
	}

	private fun addIdentifier(string: String, e: Element): Int {
		return addConstant(SLString(string), e)
	}

	override fun visitBinaryExpr(expr: Expr.Binary) {
		compile(expr.left)
		compile(expr.right)

		when (expr.operator.type) {
			MINUS -> {
				emitByte(Opcodes.SUB, expr)
			}
			SLASH -> {
				emitByte(Opcodes.DIVIDE, expr)
			}
			STAR -> {
				emitByte(Opcodes.MULTIPLY, expr)
			}
			PLUS -> {
				emitByte(Opcodes.ADD, expr)
			}
			GREATER -> {
				emitByte(Opcodes.GREATER, expr)
			}
			GREATER_EQUAL -> {
				emitBytes(Opcodes.LESS, Opcodes.NOT, expr)
			}
			LESS -> {
				emitByte(Opcodes.LESS, expr)
			}
			LESS_EQUAL -> {
				emitBytes(Opcodes.GREATER, Opcodes.NOT, expr)
			}
			BANG_EQUAL -> {
				emitBytes(Opcodes.EQUAL, Opcodes.NOT, expr)
			}
			EQUAL_EQUAL -> {
				emitByte(Opcodes.EQUAL, expr)
			}
			IS -> {
				TODO("Not yet implemented")
			}
			IS_NOT -> {
				TODO("Not yet implemented")
			}
			else -> return
		}
	}

	override fun visitGroupingExpr(expr: Expr.Grouping) {
		compile(expr.expression)
	}

	override fun visitUnaryExpr(expr: Expr.Unary) {
		compile(expr.right)
		when (expr.operator.type) {
			MINUS -> {
				emitByte(Opcodes.SUB, expr)
			}
			BANG -> {
				emitByte(Opcodes.NOT, expr)
			}
			else -> return
		}
	}

	override fun visitLiteralExpr(expr: Expr.Literal) {
		when(expr.value) {
			is Boolean -> {
				when(expr.value){
					true -> emitByte(Opcodes.TRUE, expr)
					false -> emitByte(Opcodes.FALSE, expr)
				}
			}
			is Double -> {
				emitConstant(SLNumber(expr.value),expr)
			}
			is String -> {
				emitConstant(SLString(expr.value),expr)
			}
			null -> {
				emitByte(Opcodes.NIL, expr)
			}
			else -> return
		}
	}

	override fun visitVariableExpr(expr: Expr.Variable) {
		val getOp: Opcodes

		var arg = resolveLocal(expr)
		if(arg != -1){
			getOp = Opcodes.GET_LOCAL
		} else {
			arg = resolveUpvalue(enclosing, expr)
			if(arg != -1){
				getOp = Opcodes.GET_UPVALUE
			} else {
				arg = addIdentifier(expr.name.lexeme,expr)
				getOp = Opcodes.GET_GLOBAL
			}
		}
		emitByte(getOp, expr)
		emitShort(arg, expr)
	}

	private fun resolveLocal(expr: Expr.NamedExpr): Int {
		for ((i, local) in locals.withIndex()) {
			if(local.name.lexeme == expr.getNameToken().lexeme){
				if(local.depth == -1){
					sunlite.error(expr.getLine(), "Can't read local variable in its own initializer.")
				}
				return i
			}
		}

		return -1
	}

	private fun resolveUpvalue(compiler: Compiler?, expr: Expr.NamedExpr): Int {
		if(compiler == null) return -1;
		val local: Int = compiler.resolveLocal(expr)
		if(local != -1){
			compiler.locals[local].isCaptured = true
			return addUpvalue(local, true, expr)
		}

		val upvalue: Int = resolveUpvalue(compiler.enclosing, expr)
		if(upvalue != -1){
			return addUpvalue(upvalue, false, expr)
		}

		return -1
	}

	private fun addUpvalue(local: Int, isLocal: Boolean, expr: Expr.NamedExpr): Int {

		for (i in 0 until upvalues.size) {
			val upvalue = upvalues[i]
			if(upvalue.index == local && upvalue.isLocal == isLocal){
				return i
			}
		}

		if (upvalues.size >= Short.MAX_VALUE) {
			sunlite.error(expr.getNameToken(),"Too many upvalues in function.");
			return 0;
		}


		upvalues.add(Upvalue(local, isLocal))
		return upvalues.size-1
	}

	override fun visitAssignExpr(expr: Expr.Assign) {
		val setOp: Opcodes

		var arg = resolveLocal(expr)
		if(arg != -1){
			setOp = Opcodes.SET_LOCAL
		} else {
			arg = resolveUpvalue(enclosing, expr)
			if(arg != -1){
				setOp = Opcodes.SET_UPVALUE
			} else {
				arg = addIdentifier(expr.name.lexeme,expr)
				setOp = Opcodes.SET_GLOBAL
			}
		}
		compile(expr.value)
		emitByte(setOp, expr)
		emitShort(arg, expr)
	}

	override fun visitLogicalExpr(expr: Expr.Logical) {
		when(expr.operator.type){
			AND -> {
				compile(expr.left)
				val jmp = emitJump(Opcodes.JUMP_IF_FALSE, expr)
				emitByte(Opcodes.POP, expr)
				compile(expr.right)
				patchJump(jmp, expr)
			}
			OR -> {
				compile(expr.left)
				val elseJmp = emitJump(Opcodes.JUMP_IF_FALSE, expr)
				val endJmp = emitJump(Opcodes.JUMP, expr)
				patchJump(elseJmp, expr)
				emitByte(Opcodes.POP, expr)
				compile(expr.right)
				patchJump(endJmp, expr)
			}
			else -> return
		}
	}

	override fun visitCallExpr(expr: Expr.Call) {
		compile(expr.callee)
		val argCount: Int = argumentList(expr)
		emitBytes(Opcodes.CALL, argCount, expr)
	}

	private fun argumentList(expr: Expr.Call): Int {
		for (argument in expr.arguments) {
			compile(argument)
		}
		return expr.arguments.size
	}

	override fun visitLambdaExpr(expr: Expr.Lambda) {
		makeFunction(expr.function)
	}

	override fun visitGetExpr(expr: Expr.Get) {
		compile(expr.obj)
		val name = addIdentifier(expr.name.lexeme, expr)
		emitByte(Opcodes.GET_PROP, expr)
		emitShort(name, expr)
	}

	override fun visitDynamicGetExpr(expr: Expr.DynamicGet) {
		TODO("Not yet implemented")
	}

	override fun visitDynamicSetExpr(expr: Expr.DynamicSet) {
		TODO("Not yet implemented")
	}

	override fun visitSetExpr(expr: Expr.Set) {
		val name = addIdentifier(expr.name.lexeme, expr)
		compile(expr.obj)
		compile(expr.value)
		emitByte(Opcodes.SET_PROP, expr)
		emitShort(name, expr)
	}

	override fun visitThisExpr(expr: Expr.This) {
		var compiler: Compiler? = this
		while (compiler != null){
			if(compiler.currentClass != null) break
			compiler = compiler.enclosing
		}

		if(compiler == null) {
			sunlite.error(expr.getLine(), "Can't refer to 'this' outside of a class.", currentFile)
			return
		}

		compile(Expr.Variable(expr.keyword))
	}

	override fun visitSuperExpr(expr: Expr.Super) {
		var compiler: Compiler? = this
		while (compiler != null){
			if(compiler.currentClass != null) break
			compiler = compiler.enclosing
		}

		if(compiler == null) {
			sunlite.error(expr.getLine(), "Can't refer to 'super' outside of a class.", currentFile)
			return
		} else if(compiler.currentClass?.hasSuperclass == false){
			sunlite.error(expr.getLine(), "Can't refer to 'super' in a class with no superclass.", currentFile)
			return
		}

		compile(Expr.Variable(Token.identifier("this",expr.getLine(),expr.getFile())))
		compile(Expr.Variable(Token.identifier("super",expr.getLine(),expr.getFile())))
		val name = addIdentifier(expr.method.lexeme, expr)
		emitByte(Opcodes.GET_SUPER,expr)
		emitShort(name,expr)
	}

	override fun visitCheckExpr(expr: Expr.Check) {
		TODO("Not yet implemented")
	}

	override fun visitCastExpr(expr: Expr.Cast) {
		TODO("Not yet implemented")
	}

	override fun visitExprStmt(stmt: Stmt.Expression) {
		compile(stmt.expr)
		emitByte(Opcodes.POP, stmt)
	}

	override fun visitPrintStmt(stmt: Stmt.Print) {
		compile(stmt.expr)
		emitByte(Opcodes.PRINT, stmt)
	}

	override fun visitVarStmt(stmt: Stmt.Var) {
		val constantIndex = makeVariable(stmt.name, stmt)
		if(stmt.initializer != null) {
			compile(stmt.initializer)
		} else {
			emitByte(Opcodes.NIL, stmt)
		}

		defineVariable(constantIndex, stmt)
	}

	private fun defineVariable(constantIndex: Int, stmt: Stmt.NamedStmt) {
		if(localScopeDepth > 0){
			markInitialized()
			return
		}

		emitByte(Opcodes.DEF_GLOBAL, stmt)
		emitShort(constantIndex, stmt)
	}

	private fun markInitialized() {
		if(localScopeDepth == 0) return
		locals[locals.size - 1].depth = localScopeDepth
	}

	private fun makeVariable(token: Token, stmt: Stmt.NamedStmt): Int {
		declareVariable(token, stmt)
		if(localScopeDepth > 0) return 0;

		return addConstant(SLString(token.lexeme),stmt)
	}

	private fun declareVariable(token: Token, stmt: Stmt.NamedStmt){
		if(localScopeDepth == 0) return

		for (local in locals) {
			if(local.depth != -1 && local.depth < localScopeDepth) break

			if(local.name.lexeme == token.lexeme) {
				sunlite.error(stmt.getLine(), "A variable with this name already exists in this scope.")
			}
		}

		addLocal(token, stmt)
	}

	private fun addLocal(token: Token, stmt: Stmt.NamedStmt) {
		if(locals.size >= Short.MAX_VALUE){
			sunlite.error(stmt.getLine(), "Too many local variables in function.")
			return
		}

		locals.add(Local(token, -1))
	}


	override fun visitBlockStmt(stmt: Stmt.Block) {
		beginScope(stmt)
		stmt.statements.forEach { compile(it) }
		endScope(stmt)
	}

	private fun beginScope(e: Element){
		localScopeDepth++
	}

	private fun endScope(e: Element){
		localScopeDepth--
		locals.removeIf {
			if(it.depth > localScopeDepth){
				emitByte(Opcodes.POP, e)
				return@removeIf true
			}
			return@removeIf false
		}
	}

	override fun visitIfStmt(stmt: Stmt.If) {
		compile(stmt.condition)
		val thenJump = emitJump(Opcodes.JUMP_IF_FALSE, stmt)
		emitByte(Opcodes.POP, stmt)
		compile(stmt.thenBranch)
		val elseJump = emitJump(Opcodes.JUMP, stmt)
		patchJump(thenJump, stmt)
		emitByte(Opcodes.POP, stmt)
		if(stmt.elseBranch != null) {
			compile(stmt.elseBranch)
		}
		patchJump(elseJump, stmt)
	}

	override fun visitWhileStmt(stmt: Stmt.While) {
		val loopStart = chunk.code.size
		compile(stmt.condition)

		val exitJump = emitJump(Opcodes.JUMP_IF_FALSE, stmt)
		emitByte(Opcodes.POP, stmt)
		val body: Stmt.Block = stmt.body as Stmt.Block
		if(body.statements.isNotEmpty()) {
			compile(body.statements[0])
			if( //for loop
				body.statements.size == 2 &&
				body.statements[0] is Stmt.Block &&
				body.statements[1] is Stmt.Expression &&
				(body.statements[1] as Stmt.Expression).expr is Expr.Assign
			) {
				incompleteContinues.forEach {
					patchJump(it, stmt)
				}
				incompleteContinues.clear()
				compile(body.statements[1])
			} else { //normal while loop
				val list = body.statements.subList(1, body.statements.size)
				list.forEach { compile(it) }
				incompleteContinues.forEach {
					patchJump(it, stmt)
				}
				incompleteContinues.clear()
			}
		}

		emitLoop(loopStart, stmt)

		patchJump(exitJump, stmt)
		emitByte(Opcodes.POP, stmt)

		incompleteBreaks.forEach { patchJump(it, stmt) }
		incompleteBreaks.clear()
	}

	override fun visitBreakStmt(stmt: Stmt.Break) {
		incompleteBreaks.add(emitJump(Opcodes.JUMP, stmt))
	}

	override fun visitContinueStmt(stmt: Stmt.Continue) {
		incompleteContinues.add(emitJump(Opcodes.JUMP, stmt))
	}

	override fun visitFunctionStmt(stmt: Stmt.Function) {
		val constantIndex: Int = addIdentifier(stmt.name.lexeme,stmt)
		markInitialized()
		makeFunction(stmt)
		if(stmt.type == FunctionType.FUNCTION){
			defineVariable(constantIndex, stmt)
		}
	}

	private fun makeFunction(stmt: Stmt.Function){
		val compiler = Compiler(sunlite,vm,this)
		compiler.beginScope(stmt)
		for (param in stmt.params) {
			val constantIndex = compiler.makeVariable(param.token, stmt)
			compiler.defineVariable(constantIndex, stmt)
		}

		val function: SLFunction = compiler.compile(stmt.type, stmt.body, currentFile, stmt.name.lexeme, stmt.params.size)
		emitByte(Opcodes.CLOSURE, stmt)
		emitShort(addConstant(SLFuncObj(function),stmt),stmt)
		for (upvalue in compiler.upvalues) {
			emitByte(if(upvalue.isLocal) 1 else 0, stmt)
			emitShort(upvalue.index, stmt)
		}
	}

	override fun visitReturnStmt(stmt: Stmt.Return) {
		if(currentFunctionType == FunctionType.INITIALIZER){
			sunlite.error(stmt.getLine(), "Can't explicitly return from an initializer.", currentFile)
		}
		if(stmt.value == null){
			emitByte(Opcodes.NIL, stmt)
			emitByte(Opcodes.RETURN, stmt)
		} else {
			compile(stmt.value)
			emitByte(Opcodes.RETURN, stmt)
		}
	}

	override fun visitClassStmt(stmt: Stmt.Class) {
		val className = stmt.name
		val nameConstant = addIdentifier(className.lexeme, stmt)
		declareVariable(className, stmt)

		emitByte(Opcodes.CLASS, stmt)
		emitShort(nameConstant, stmt)
		defineVariable(nameConstant,stmt)

		val classCompiler = ClassCompiler(currentClass)
		currentClass = classCompiler

		if(stmt.superclass != null){
			compile(Expr.Variable(stmt.superclass.name))

			beginScope(stmt.superclass)
			addLocal(Token.identifier("super",stmt.getLine(),stmt.getFile()), stmt)
			defineVariable(0, stmt)
			compile(Expr.Variable(className))
			emitByte(Opcodes.INHERIT,stmt.superclass)
			classCompiler.hasSuperclass = true;
		}

		compile(Expr.Variable(className))

		for (method in stmt.methods) {
			val methodName = addIdentifier(method.name.lexeme, stmt)
			compile(method)
			emitByte(Opcodes.METHOD, stmt)
			emitShort(methodName,stmt)
		}

		emitByte(Opcodes.POP, stmt)

		if(currentClass?.hasSuperclass == true){
			endScope(stmt)
		}

		currentClass = currentClass?.enclosing
	}

	override fun visitInterfaceStmt(stmt: Stmt.Interface) {
		TODO("Not yet implemented")
	}

	override fun visitImportStmt(stmt: Stmt.Import) {
		TODO("Not yet implemented")
	}
}