package sunsetsatellite.vm.lox

class LoxClosure(val function: LoxFunction, val upvalues: Array<LoxUpvalue?> = arrayOfNulls(function.upvalueCount)) {

	override fun toString(): String {
		return function.toString()
	}
}