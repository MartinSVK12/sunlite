package sunsetsatellite.sunlite.vm

import sunsetsatellite.sunlite.lang.Descriptor
import sunsetsatellite.sunlite.lang.FunctionModifier
import sunsetsatellite.sunlite.lang.Param
import sunsetsatellite.sunlite.lang.Token
import sunsetsatellite.sunlite.lang.Type
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class SLFunction(
    val name: String,
    val returnType: Type,
    val params: List<Param>,
    val typeParams: List<Param>,
    val chunk: Chunk,
    val arity: Int = 0,
    val upvalueCount: Int,
    val localsCount: Int,
    val modifier: Array<FunctionModifier> = arrayOf(FunctionModifier.NORMAL)
) {

    companion object {
        fun read(s: DataInputStream): SLFunction {
	        if (s.readInt() != 0x51C0DE) {
                throw IOException("Invalid data in stream.")
	        }
            val name = s.readUTF()
            val returnType = Descriptor(s.readUTF()).getType()
            val paramsSize = s.readInt()
            val params = mutableListOf<Param>()
	        (0 until paramsSize).forEach { _ ->
		        val paramName = s.readUTF()
		        val paramType = Descriptor(s.readUTF()).getType()
		        params.add(Param(Token.identifier(paramName), paramType))
	        }
            val typeParamsSize = s.readInt()
            val typeParams = mutableListOf<Param>()
	        (0 until typeParamsSize).forEach { _ ->
		        val typeParamName = s.readUTF()
		        val typeParamType = Descriptor(s.readUTF()).getType()
		        params.add(Param(Token.identifier(typeParamName), typeParamType))
	        }
            val chunk = Chunk.read(s)
            val arity = s.readInt()
            val upvalueCount = s.readInt()
            val localsCount = s.readInt()
            val modCount = s.readInt()
            val modifier = mutableListOf<FunctionModifier>()
            for (i in 0 until modCount) {
                modifier.add(FunctionModifier.entries[s.readInt()])
            }
            return SLFunction(name, returnType, params, typeParams, chunk, arity, upvalueCount, localsCount, modifier.toTypedArray())
        }
    }

    override fun toString(): String {
        return "<function '${name.replace(Regex("\\(.*\\).*;"),"")}(${params.map { it.type }.joinToString()}): ${returnType}'>"
    }

    fun copy(): SLFunction {
        return SLFunction(name, returnType, params, typeParams, chunk, arity, upvalueCount, localsCount, modifier)
    }

    fun write(s: DataOutputStream){
        s.writeInt(0x51C0DE)
        s.writeUTF(name)
        s.writeUTF(returnType.getDescriptor())
        s.writeInt(params.size)
        for (param in params) {
            s.writeUTF(param.token.lexeme)
            s.writeUTF(param.type.getDescriptor())
        }
        s.writeInt(typeParams.size)
        for (param in typeParams) {
            s.writeUTF(param.token.lexeme)
            s.writeUTF(param.type.getDescriptor())
        }
        chunk.write(s)
        s.writeInt(arity)
        s.writeInt(upvalueCount)
        s.writeInt(localsCount)
        s.write(modifier.size)
        for (mod in modifier) {
            s.writeInt(mod.ordinal)
        }
    }
}