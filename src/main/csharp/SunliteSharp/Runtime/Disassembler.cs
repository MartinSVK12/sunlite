using System.Text;
using SunliteSharp.Util;

namespace SunliteSharp.Runtime;

public static class Disassembler
    {
        public static string DisassembleChunk(Chunk chunk)
        {
            var sb = new StringBuilder();
            sb.Append($"==== {chunk.Header.File}::{chunk.Header.Name} ====\n");

            var offset = 0;
            while (offset < chunk.Length())
            {
                offset = DisassembleInstruction(sb, chunk, offset);
            }

            sb.Append("==== Exception Table ====\n");
            var idx = 0;
            foreach (KeyValuePair<IntRange, IntRange> entry in chunk.Exceptions)
            {
                sb.Append(entry.Key).Append(" -> ").Append(entry.Value).Append('\n');
                idx++;
            }

            sb.Append($"==== {chunk.Header.File}::{chunk.Header.Name} ====\n");
            return sb.ToString();
        }

        public static int DisassembleInstruction(StringBuilder sb, Chunk chunk, int offset)
        {
            sb.Append($"{offset:0000} ");
            if (offset > 0 && chunk.Header.Lines[offset] == chunk.Header.Lines[offset - 1])
            {
                sb.Append("   | ");
            }
            else
            {
                sb.Append($"{chunk.Header.Lines[offset],4} ");
            }

            var inst = chunk.Code[offset];
            var opcode = (Opcodes)inst;

            return opcode switch
            {
                Opcodes.Nop or Opcodes.Return => SimpleInstruction(sb, opcode.ToString(), offset),
                Opcodes.Constant => ConstantInstruction(sb, opcode.ToString(), chunk, offset),
                Opcodes.Negate => SimpleInstruction(sb, opcode.ToString(), offset),
                Opcodes.Add => SimpleInstruction(sb, opcode.ToString(), offset),
                Opcodes.Sub => SimpleInstruction(sb, opcode.ToString(), offset),
                Opcodes.Multiply => SimpleInstruction(sb, opcode.ToString(), offset),
                Opcodes.Divide => SimpleInstruction(sb, opcode.ToString(), offset),
                Opcodes.Nil => SimpleInstruction(sb, opcode.ToString(), offset),
                Opcodes.True => SimpleInstruction(sb, opcode.ToString(), offset),
                Opcodes.False => SimpleInstruction(sb, opcode.ToString(), offset),
                Opcodes.Not => SimpleInstruction(sb, opcode.ToString(), offset),
                Opcodes.Equal => SimpleInstruction(sb, opcode.ToString(), offset),
                Opcodes.Greater => SimpleInstruction(sb, opcode.ToString(), offset),
                Opcodes.Less => SimpleInstruction(sb, opcode.ToString(), offset),
                Opcodes.Pop => SimpleInstruction(sb, opcode.ToString(), offset),
                Opcodes.DefGlobal => ConstantInstruction(sb, opcode.ToString(), chunk, offset),
                Opcodes.SetGlobal => ConstantInstruction(sb, opcode.ToString(), chunk, offset),
                Opcodes.GetGlobal => ConstantInstruction(sb, opcode.ToString(), chunk, offset),
                Opcodes.SetLocal or Opcodes.GetLocal => ShortInstruction(sb, opcode.ToString(), chunk, offset),
                Opcodes.JumpIfFalse or Opcodes.Jump => JumpInstruction(sb, opcode.ToString(), 1, chunk, offset),
                Opcodes.Loop => JumpInstruction(sb, opcode.ToString(), -1, chunk, offset),
                Opcodes.Call => TwoByteInstruction(sb, opcode.ToString(), chunk, offset),
                Opcodes.Closure => ClosureInstruction(sb, opcode.ToString(), chunk, offset),
                Opcodes.GetUpvalue or Opcodes.SetUpvalue => ShortInstruction(sb, opcode.ToString(), chunk, offset),
                Opcodes.Class => ConstantInstruction(sb, opcode.ToString(), chunk, offset),
                Opcodes.SetProp => ConstantInstruction(sb, opcode.ToString(), chunk, offset),
                Opcodes.GetProp => ConstantInstruction(sb, opcode.ToString(), chunk, offset),
                Opcodes.Method => ConstantInstruction(sb, opcode.ToString(), chunk, offset),
                Opcodes.Field => ConstantInstruction(sb, opcode.ToString(), chunk, offset),
                Opcodes.StaticField => ConstantInstruction(sb, opcode.ToString(), chunk, offset),
                Opcodes.Inherit => SimpleInstruction(sb, opcode.ToString(), offset),
                Opcodes.GetSuper => ConstantInstruction(sb, opcode.ToString(), chunk, offset),
                Opcodes.ArrayGet => SimpleInstruction(sb, opcode.ToString(), offset),
                Opcodes.ArraySet => SimpleInstruction(sb, opcode.ToString(), offset),
                Opcodes.Throw => SimpleInstruction(sb, opcode.ToString(), offset),
                Opcodes.Check or Opcodes.TypeParam => ConstantInstruction(sb, opcode.ToString(), chunk, offset),
                _ => throw new ArgumentOutOfRangeException(nameof(offset),"Invalid opcode")
            };
        }

        private static int ClosureInstruction(StringBuilder sb, string name, Chunk chunk, int startOffset)
        {
            var offset = startOffset;
            var constant = (chunk.Code[offset + 1] << 8) | chunk.Code[offset + 2];
            offset += 3;
            sb.Append($"{name,-16} ({(int)Enum.Parse<Opcodes>(name):X2}) {constant,4} ");
            sb.Append(constant > chunk.Constants.Length
                ? "<error reading constant>"
                : chunk.Constants[constant]?.ToString());
            sb.Append('\n');

            var function = (SLFuncObj)chunk.Constants[constant];
            for (var i = 0; i < function.Value.UpvalueCount; i++)
            {
                int isLocal = chunk.Code[offset++];
                var index = (chunk.Code[offset] << 8) | chunk.Code[offset + 1];
                offset += 2;
                sb.Append($"{offset - 3:0000}      L  {((isLocal == 1) ? "local" : "upvalue")} {index}\n");
            }
            return offset;
        }

        private static int JumpInstruction(StringBuilder sb, string name, int sign, Chunk chunk, int offset)
        {
            var jmp = (chunk.Code[offset + 1] << 8) | chunk.Code[offset + 2];
            sb.Append(
                $"{name,-16} ({(int)Enum.Parse<Opcodes>(name):X2}) {offset,4} -> {offset + 3 + sign * jmp}\n");
            return offset + 3;
        }

        private static int ByteInstruction(StringBuilder sb, string name, Chunk chunk, int offset)
        {
            var b = chunk.Code[offset + 1];
            sb.Append($"{name,-16} ({(int)Enum.Parse<Opcodes>(name):X2}) {b,4}\n");
            return offset + 2;
        }

        private static int TwoByteInstruction(StringBuilder sb, string name, Chunk chunk, int offset)
        {
            var b1 = chunk.Code[offset + 1];
            var b2 = chunk.Code[offset + 2];
            sb.Append($"{name,-16} ({(int)Enum.Parse<Opcodes>(name):X2}) {b1,4} {b2,4}\n");
            return offset + 2;
        }

        private static int ShortInstruction(StringBuilder sb, string name, Chunk chunk, int offset)
        {
            var val = (chunk.Code[offset + 1] << 8) | chunk.Code[offset + 2];
            sb.Append($"{name,-16} ({(int)Enum.Parse<Opcodes>(name):X2}) {val,4}\n");
            return offset + 3;
        }

        private static int ConstantInstruction(StringBuilder sb, string name, Chunk chunk, int offset)
        {
            var constant = (chunk.Code[offset + 1] << 8) | chunk.Code[offset + 2];
            sb.Append($"{name,-16} ({(int)Enum.Parse<Opcodes>(name):X2}) {constant,4} ");
            sb.Append(constant > chunk.Constants.Length
                ? "<error reading constant>"
                : chunk.Constants[constant]?.ToString());
            sb.Append('\n');
            return offset + 3;
        }

        private static int SimpleInstruction(StringBuilder sb, string name, int offset)
        {
            sb.Append($"{name,-16} ({(int)Enum.Parse<Opcodes>(name):X2})\n");
            return offset + 1;
        }
    }