package sunsetsatellite.vm.sunlite

import java.util.*

class CallFrame(val closure: SunliteClosure/*, val locals: Array<AnyLoxValue>*/) {
	var pc: Int = 0
	val stack: Stack<AnyLoxValue> = Stack()

	fun pop(): AnyLoxValue {
		return stack.pop()
	}

	fun push(value: AnyLoxValue) {
		stack.push(value)
	}

	fun peek(): AnyLoxValue {
		return stack.peek()
	}

	fun peek(i: Int): AnyLoxValue {
		val len = stack.size

		if (len == 0) throw EmptyStackException()
		return stack.elementAt(len - i - 1)
	}
}