package sunsetsatellite.interpreter.sunlite

import sunsetsatellite.lang.sunlite.FunctionModifier
import sunsetsatellite.lang.sunlite.Sunlite
import sunsetsatellite.lang.sunlite.Stmt
import sunsetsatellite.lang.sunlite.Type

class LoxFunction(
	val declaration: Stmt.Function,
	val closure: Environment,
	val sunlite: Sunlite
) : LoxCallable {

	override fun call(
		interpreter: Interpreter,
		arguments: List<Any?>?,
		typeArguments: List<Type>
	): Any? {
		val environment = Environment(closure,declaration.getLine(),signature(),declaration.getFile())
		for (i in declaration.params.indices) {
			environment.define(
				declaration.params[i].token.lexeme,
				arguments?.get(i)
			)
		}

		try {
			if(declaration.modifier == FunctionModifier.NATIVE) {
				val thisClass = closure.getAt(0, "this") as LoxClassInstance?
				return sunlite
					.natives[signature()]
					?.call(interpreter, arguments, thisClass) ?: throw LoxRuntimeError(declaration.name, "Native function not bound to anything,")
			} else {
				interpreter.executeBlock(declaration.body, environment)
			}
		} catch (returnValue: LoxReturn) {
			if (declaration.modifier == FunctionModifier.INIT) return closure.getAt(0, "this");
			return returnValue.value
		}

		if (declaration.modifier == FunctionModifier.INIT) return closure.getAt(0, "this");

		return null
	}

	override fun arity(): Int {
		return declaration.params.size
	}

	override fun typeArity(): Int {
		return declaration.typeParams.size
	}

	override fun signature(): String {
		val thisClass = closure.getAt(0, "this") as LoxClassInstance?
		return "${thisClass?.name() ?: ""}${if(thisClass != null) "::" else ""}${declaration.name.lexeme}(${if(declaration.typeParams.isNotEmpty()) "<${declaration.typeParams.joinToString(", ")}>" else ""} ${declaration.params.joinToString(", ")} ${if(declaration.returnType != Type.NIL) ": ${declaration.returnType}" else ""})"
	}

	override fun varargs(): Boolean {
		//TODO: implement
		return false
	}

	fun bind(instance: LoxClassInstance): LoxFunction {
		if(declaration.modifier == FunctionModifier.STATIC) {
			throw LoxRuntimeError(declaration.name, "Cannot bind 'this' to a static function.")
		}
		val environment = Environment(closure,declaration.getLine(),"this::${signature()}",declaration.getFile())
		environment.define("this", instance)
		return LoxFunction(declaration, environment, sunlite)
	}


	override fun toString(): String {
		return "<${declaration.modifier.name.lowercase()} fn '${declaration.name.lexeme}'>"
	}

	fun setModifier(modifier: FunctionModifier): LoxFunction {
		declaration.modifier = modifier
		return this
	}

}