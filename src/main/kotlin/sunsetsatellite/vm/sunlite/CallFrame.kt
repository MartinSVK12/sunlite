package sunsetsatellite.vm.sunlite

import com.sun.org.apache.xalan.internal.lib.ExsltDynamic.closure
import java.util.*

class CallFrame(val closure: SLClosure, val locals: MutableList<AnySLValue>) {
	var pc: Int = 0
	val stack: Stack<AnySLValue> = Stack()

	fun pop(): AnySLValue {
		return stack.pop()
	}

	fun push(value: AnySLValue) {
		stack.push(value)
	}

	fun peek(): AnySLValue {
		return stack.peek()
	}

	fun peek(i: Int): AnySLValue {
		val len = stack.size

		if (len == 0) throw EmptyStackException()
		return stack.elementAt(len - i - 1)
	}

	override fun toString(): String {
		return "[line ${closure.function.chunk.debugInfo.lines[pc]}] in ${if(closure.function.name == "") "script" else "${closure.function.name}()"}"
	}
}