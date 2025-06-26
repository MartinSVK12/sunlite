package sunsetsatellite.vm.sunlite

import java.util.*

class CallFrame(val closure: SunliteClosure/*, val locals: Array<AnyLoxValue>*/) {
	var pc: Int = 0
	val stack: Stack<AnySunliteValue> = Stack()

	fun pop(): AnySunliteValue {
		return stack.pop()
	}

	fun push(value: AnySunliteValue) {
		stack.push(value)
	}

	fun peek(): AnySunliteValue {
		return stack.peek()
	}

	fun peek(i: Int): AnySunliteValue {
		val len = stack.size

		if (len == 0) throw EmptyStackException()
		return stack.elementAt(len - i - 1)
	}
}