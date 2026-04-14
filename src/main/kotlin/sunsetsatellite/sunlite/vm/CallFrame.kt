package sunsetsatellite.sunlite.vm

import java.util.*
import kotlin.io.path.Path

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
        val line = closure.function.chunk.debugInfo.lines[Math.min(pc, closure.function.chunk.debugInfo.lines.size-1)]
        val name = closure.function.name
        val file = closure.function.chunk.debugInfo.originalFile[line]
        return "${if (name == "") "<script>" else name} in ${if(file != null) Path(file).fileName else "<unknown file>"}:${line}"
        //return "[line ${closure.function.chunk.debugInfo.lines[pc]}] in ${if (closure.function.name == "") "${closure.function.chunk.debugInfo.file}" else "${closure.function.name}()"}"
    }
}