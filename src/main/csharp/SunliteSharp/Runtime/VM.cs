using System.Text;
using SunliteSharp.Core;
using SunliteSharp.Core.AST;
using SunliteSharp.Core.Compiler;
using SunliteSharp.Core.Enum;
using SunliteSharp.Core.Modifier;
using SunliteSharp.Util;

namespace SunliteSharp.Runtime;

using Type = Core.Type;

public class VM
{
    
    public VM(Sunlite sl, string[] launchArgs)
    {
        Sl = sl;
        LaunchArgs = launchArgs;
        Sl.Natives.RegisterNatives(this);
        Checker = new TypeChecker(Sl, this);
    }
    
    public const int MaxFrames = 255;
    
    public static AnySLValue[] ArrayOfNils(int size)
    {
        return Enumerable.Repeat(SLNil.Nil, size).ToArray<AnySLValue>();
    }

    public Dictionary<string, AnySLValue> Globals { get; set; } = [];
    public List<SLUpvalue> OpenUpvalues { get; set; } = [];

    public CallFrame? CurrentFrame = null;
    public Stack<CallFrame> FrameStack = [];
    public AnySLValue? CurrentException = null;
    public List<CallFrame> ExceptionStacktrace = [];
    public readonly Sunlite Sl;
    public readonly string[] LaunchArgs;
    public readonly TypeChecker Checker;

    public void DefineNative(SLNativeFunction function)
    {
        Globals[function.Name] = new SLNativeFuncObj(function);
    }

    public void Run()
    {
        var fr = FrameStack.Peek();
        CurrentFrame = fr;

        while (fr.Pc < fr.Closure.Function.Chunk.Code.Length)
        {
            var CurrentLine = fr.Closure.Function.Chunk.Code[fr.Pc];
            var CurrentFile = fr.Closure.Function.Chunk.Header.File;

            Tick();
        }
    }

    public void Tick()
    {
        try
        {
            if (CurrentFrame is null)
            {
                Sl.PrintErr("VM is uninitialized!");
                return;
            }
            var fr = CurrentFrame;
            if (fr.Pc < fr.Closure.Function.Chunk.Code.Length)
            {
                if (Sunlite.BytecodeDebug)
                {
                    var sb = new StringBuilder();

                    sb.Append($"STACK @ {fr.Closure.Function.Chunk.Header.File.Split('\\').Last()}::{fr.Closure.Function.Chunk.Header.Name}: ");
                    foreach (var value in fr.Stack)
                    {
                        sb.Append("[ ");
                        sb.Append(value);
                        sb.Append(" ]");
                    }
                    if (fr.Stack.Count == 0)
                    {
                        sb.Append("[ ]");
                    }
                    sb.Append('\n');

                    sb.Append($"LOCALS @ {fr.Closure.Function.Chunk.Header.File.Split('\\').Last()}::{fr.Closure.Function.Chunk.Header.Name}: ");
                    foreach (var value in fr.Locals)
                    {
                        sb.Append("[ ");
                        sb.Append(value);
                        sb.Append(" ]");
                    }
                    if (fr.Locals.Count == 0)
                    {
                        sb.Append("[ ]");
                    }
                    sb.Append('\n');

                    Disassembler.DisassembleInstruction(sb, fr.Closure.Function.Chunk, fr.Pc);
                    Sl.PrintInfo(sb.ToString());
                }

                var instruction = ReadByte(fr);
                switch ((Opcodes)instruction)
                {
                    case Opcodes.Nop:
                    {
                        break;
                    }
                    case Opcodes.Return:
                    {
                        var value = fr.Pop();
                        if (FrameStack.Count == 1)
                        {
                            return;
                        }
                        var previousFrame = FrameStack.Pop();
                        fr = FrameStack.Peek();
                        CurrentFrame = fr;
                        for (var i = 0; i < previousFrame.Closure.Function.Arity + 1; i++)
                        {
                            fr.Pop();
                        }
                        fr.Push(value);
                        break;
                    }
                    case Opcodes.Constant:
                    {
                        fr.Push(ReadConstant(fr));
                        break;
                    }
                    case Opcodes.Negate:
                    {
                        if (fr.Peek() is not SLNumber number)
                        {
                            RuntimeError("Operand must be a number.");
                            return;
                        }
                        fr.Push(-number);
                        break;
                    }
                    case Opcodes.Add:
                    {
                        if (fr.Peek() is SLNumber number2 && fr.Peek(1) is SLNumber number)
                        {
                            fr.Pop();
                            fr.Pop();
                            fr.Push(number + number2);
                        } else if (fr.Peek() is SLString slString2 && fr.Peek(1) is SLString slString)
                        {
                            fr.Pop();
                            fr.Pop();
                            fr.Push(slString + slString2);
                        }
                        else
                        {
                            RuntimeError("Operands must be numbers or strings.");
                            return;
                        }
                        break;
                    }
                    case Opcodes.Sub:
                    {
                        if (fr.Peek() is SLNumber number2 && fr.Peek(1) is SLNumber number)
                        {
                            fr.Pop();
                            fr.Pop();
                            fr.Push(number - number2);
                        }
                        else
                        {
                            RuntimeError("Operands must be numbers.");
                            return;
                        }
                        break;
                    }
                    case Opcodes.Multiply:
                    {
                        if (fr.Peek() is SLNumber number2 && fr.Peek(1) is SLNumber number)
                        {
                            fr.Pop();
                            fr.Pop();
                            fr.Push(number * number2);
                        }
                        else
                        {
                            RuntimeError("Operands must be numbers.");
                            return;
                        }
                        break;
                    }
                    case Opcodes.Divide:
                    {
                        if (fr.Peek() is SLNumber number2 && fr.Peek(1) is SLNumber number)
                        {
                            fr.Pop();
                            fr.Pop();
                            fr.Push(number / number2);
                        }
                        else
                        {
                            RuntimeError("Operands must be numbers.");
                            return;
                        }
                        break;
                    }
                    case Opcodes.Nil:
                    {
                        fr.Push(SLNil.Nil);
                        break;
                    }
                    case Opcodes.True:
                    {
                        fr.Push(SLBool.True);
                        break;
                    }
                    case Opcodes.False:
                    {
                        fr.Push(SLBool.False);
                        break;
                    }
                    case Opcodes.Not:
                    {
                        fr.Push(new SLBool(IsFalse(fr.Pop())));
                        break;
                    }
                    case Opcodes.Equal:
                    {
                        fr.Push(new SLBool(fr.Pop().Equals(fr.Pop())));
                        break;
                    }
                    case Opcodes.Greater:
                    {
                        if (fr.Peek() is SLNumber number && fr.Peek(1) is SLNumber number2)
                        {
                            fr.Pop();
                            fr.Pop();
                            fr.Push(number > number2);
                        }
                        else
                        {
                            RuntimeError("Operands must be numbers.");
                            return;
                        }
                        break;
                    }
                    case Opcodes.Less:
                    {
                        if (fr.Peek() is SLNumber number && fr.Peek(1) is SLNumber number2)
                        {
                            fr.Pop();
                            fr.Pop();
                            fr.Push(number < number2);
                        }
                        else
                        {
                            RuntimeError("Operands must be numbers.");
                            return;
                        }
                        break;
                    }
                    case Opcodes.Pop:
                    {
                        fr.Pop();
                        break;
                    }
                    case Opcodes.DefGlobal:
                    {
                        var name = ReadString(fr);
                        Globals[name.Value] = fr.Pop();
                        break;
                    }
                    case Opcodes.SetGlobal:
                    {
                        var name = ReadString(fr);
                        if (!Globals.ContainsKey(name.Value))
                        {
                            RuntimeError($"Undefined variable '{name.Value}'.");
                            return;
                        }
                        Globals[name.Value] = fr.Peek();
                        break;
                    }
                    case Opcodes.GetGlobal:
                    {
                        var name = ReadString(fr);
                        if (!Globals.TryGetValue(name.Value, out AnySLValue? value))
                        {
                            RuntimeError($"Undefined variable '{name.Value}'.");
                            return;
                        }
                        fr.Push(value);
                        break;
                    }
                    case Opcodes.SetLocal:
                    {
                        fr.Locals[ReadShort(fr)] = fr.Peek();
                        break;
                    }
                    case Opcodes.GetLocal:
                    {
                        fr.Push(fr.Locals[ReadShort(fr)]);
                        break;
                    }
                    case Opcodes.JumpIfFalse:
                    {
                        var offset = ReadShort(fr);
                        if (IsFalse(fr.Peek()))
                        {
                            fr.Pc += offset;
                        }
                        break;
                    }
                    case Opcodes.Jump:
                    {
                        fr.Pc = ReadShort(fr);
                        break;
                    }
                    case Opcodes.Loop:
                    {
                        fr.Pc -= ReadShort(fr);
                        break;
                    }
                    case Opcodes.Call:
                    {
                        var argCount = ReadByte(fr);
                        var typeArgCount = ReadByte(fr);
                        var callee = fr.Peek(argCount + typeArgCount);
                        if (callee is SLString or SLArrayObj or SLTableObj)
                        {
                            argCount++;
                            callee = fr.Peek(argCount + typeArgCount);
                        }

                        if (!CallValue(callee, argCount, typeArgCount))
                        {
                            return;
                        }

                        fr = FrameStack.Peek();
                        CurrentFrame = fr;
                        break;
                    }
                    case Opcodes.Closure:
                    {
                        var function = (ReadConstant(fr) as SLFuncObj)!;
                        var closure = new SLClosure(function.Value, new SLUpvalue?[function.Value.UpvalueCount]);
                        fr.Push(new SLClosureObj(closure));
                        for (var i = 0; i < closure.Upvalues.Length; i++)
                        {
                            var isLocal = ReadByte(fr);
                            var index = ReadShort(fr);
                            if (isLocal == 1)
                            {
                                closure.Upvalues[i] = CaptureUpvalue(fr, index);
                            }
                            else
                            {
                                closure.Upvalues[i] = fr.Closure.Upvalues[index];
                            }
                            
                        }
                        break;
                    }
                    case Opcodes.GetUpvalue:
                    {
                        var index = ReadShort(fr);
                        fr.Push(fr.Closure.Upvalues[index]?.ClosedValue ?? SLNil.Nil);
                        break;
                    }
                    case Opcodes.SetUpvalue:
                    {
                        var index = ReadShort(fr);
                        var upvalue = fr.Closure.Upvalues[index];
                        if (upvalue is not null) upvalue.ClosedValue = fr.Peek(0);
                        break;
                    }
                    case Opcodes.Class:
                    {
                        var name = ReadString(fr);
                        var isAbstract = (fr.Pop() as SLBool)!;
                        fr.Push(new SLClassObj(new SLClass(name.Value, [], [], [], [], isAbstract.Value)));
                        break;
                    }
                    case Opcodes.SetProp:
                    {
                        if (fr.Peek(1) is not SLClassInstanceObj && fr.Peek(1) is not SLClassObj)
                        {
                            RuntimeError("Only classes or class instances have properties.");
                            return;
                        }
                        var name = ReadString(fr).Value;
                        var value = fr.Pop();
                        switch (fr.Peek(1).Get())
                        {
                            case SLClass clazz:
                                //TODO: type check
                                clazz.StaticFields[name].Value = value;
                                fr.Pop();
                                fr.Push(value);
                                break;
                            case SLClassInstance instance:
                                //TODO: type check
                                instance.Fields[name].Value = value;
                                fr.Pop();
                                fr.Push(value);
                                break;
                        }
                        break;
                    }
                    case Opcodes.GetProp:
                    {
                        if (fr.Peek(0) is not SLClassInstanceObj
                            && fr.Peek(0) is not SLClassObj
                            && fr.Peek(0) is not SLString
                            && fr.Peek(0) is not SLArrayObj
                            && fr.Peek(0) is not SLTableObj)
                        {
                            RuntimeError("Only classes or class instances have properties.");
                            return;
                        }
                        var name = ReadString(fr).Value;
                        switch (fr.Peek(0).Get())
                        {
                            case SLClass clazz:
                            {
                                if (clazz.StaticFields.TryGetValue(name, out var field))
                                {
                                    fr.Pop();
                                    fr.Push(field.Value);
                                } else if (clazz.Methods.TryGetValue(name, out var method) &&
                                           method.Value.Function.Modifier == FunctionModifier.Static)
                                {
                                    fr.Pop();
                                    fr.Push(method);
                                } else if (method is not null && method.Value.Function.Modifier == FunctionModifier.StaticNative &&
                                           BindMethod(fr, clazz, name))
                                {
                                        
                                }
                                else
                                {
                                    RuntimeError($"Undefined static property '{name}'.");
                                    return;
                                }
                                break;
                            }
                            case SLClassInstance instance:
                            {
                                if (instance.Fields.TryGetValue(name, out var field))
                                {
                                    fr.Pop();
                                    fr.Push(field.Value);
                                } else if (BindMethod(fr, instance.Clazz, name))
                                {
                                    
                                }
                                else
                                {
                                    RuntimeError($"Undefined property '{name}'.");
                                    return;
                                }
                                break;
                            }
                            case SLString str:
                            {
                                if (Globals.TryGetValue($"string#{name}", out var value))
                                {
                                    fr.Pop();
                                    fr.Push(value);
                                    fr.Push(str);
                                }
                                else
                                {
                                    RuntimeError($"Undefined property '{name}'.");
                                    return;
                                }
                                break;
                            }
                            case SLArray arr:
                            {
                                if (Globals.TryGetValue("array", out var value) && value.Get() is SLClass clazz)
                                {
                                    if (!clazz.Methods.TryGetValue(name, out var method))
                                    {
                                        RuntimeError($"Undefined property '{name}'.");
                                        return;
                                    }

                                    fr.Pop();
                                    fr.Push(method);
                                    fr.Push(new SLArrayObj(arr));
                                }
                                else
                                {
                                    RuntimeError("Internal VM Error: Missing internal class 'array'.");
                                }
                                break;
                            }
                            case SLTable table:
                            {
                                if (Globals.TryGetValue("table", out var value) && value.Get() is SLClass clazz)
                                {
                                    if (!clazz.Methods.TryGetValue(name, out var method))
                                    {
                                        RuntimeError($"Undefined property '{name}'.");
                                        return;
                                    }

                                    fr.Pop();
                                    fr.Push(method);
                                    fr.Push(new SLTableObj(table));
                                }
                                else
                                {
                                    RuntimeError("Internal VM Error: Missing internal class 'table'.");
                                    return;
                                }
                                break;
                            }
                        }
                        break;
                    }
                    case Opcodes.Method:
                    {
                        DefineMethod(fr, ReadString(fr).Value);
                        break;
                    }
                    case Opcodes.Field:
                    {
                        DefineField(fr, ReadString(fr).Value);
                        break;
                    }
                    case Opcodes.StaticField:
                    {
                        DefineStaticField(fr, ReadString(fr).Value);
                        break;
                    }
                    case Opcodes.TypeParam:
                    {
                        DefineTypeParam(fr, ReadString(fr).Value);
                        break;
                    }
                    case Opcodes.Inherit:
                    {
                        var supervalue = fr.Peek(1);
                        
                        if (supervalue is not SLClassObj superclass)
                        {
                            RuntimeError("Superclass must be a class.");
                            return;
                        }
                        
                        var subvalue = fr.Peek(0);
                        if (subvalue is not SLClassObj subclass)
                        {
                            RuntimeError("Only classes support inheritance.");
                            return;
                        }
                        
                        foreach (var (key, value) in superclass.Value.Methods)
                        {
                            subclass.Value.Methods[key] = value;
                        }

                        fr.Pop();
                        
                        break;
                    }
                    case Opcodes.GetSuper:
                    {
                        var name = ReadString(fr);
                        var superclass = fr.Pop();

                        if (superclass is not SLClassObj)
                        {
                            RuntimeError("Superclass must be a class");
                            return;
                        }

                        if (!BindMethod(fr, (superclass.Get() as SLClass)!, name.Value))
                        {
                            RuntimeError($"Cannot bind method '{name.Value}' to class '{superclass.Get()}'");
                            return;
                        }
                        
                        break;
                    }
                    case Opcodes.ArraySet:
                    {
                        if (fr.Peek(0) is not SLArrayObj && fr.Peek(0) is not SLTableObj)
                        {
                            RuntimeError("Only arrays and tables support the indexing operator.");
                            return;
                        }

                        if (fr.Peek(0) is SLArrayObj array)
                        {
                            var arr = array.Value;
                            var number = fr.Pop();
                            var value = fr.Pop();
                            if (number is not SLNumber index)
                            {
                                RuntimeError("Array index must be a number.");
                                return;
                            }
                            arr[(int)index.Value] = value;
                            fr.Push(value);
                        }
                        else
                        {
                            var table = (fr.Pop() as SLTableObj)!.Value;
                            var key = fr.Pop();
                            var value = fr.Pop();
                            table[key] = value;
                            fr.Push(value);
                        }
                        break;
                    }
                    case Opcodes.ArrayGet:
                    {
                        if (fr.Peek(0) is not SLArrayObj && fr.Peek(0) is not SLTableObj)
                        {
                            RuntimeError("Only arrays and tables support the indexing operator.");
                            return;
                        }

                        if (fr.Peek(0) is SLArrayObj array)
                        {
                            fr.Pop();
                            var arr = array.Value;
                            var number = fr.Pop();
                            if (number is not SLNumber index)
                            {
                                RuntimeError("Array index must be a number.");
                                return;
                            }
                            fr.Push(arr[(int)index.Value]);
                        }
                        else
                        {
                            var table = (fr.Pop() as SLTableObj)!.Value;
                            var key = fr.Pop();
                            fr.Push(table[key]);
                        }
                        break;
                    }
                    case Opcodes.Throw:
                    {
                        ThrowException(fr.Stack.Count - 1, fr.Pop());
                        fr = FrameStack.Peek();
                        CurrentFrame = fr;
                        break;
                    }
                    case Opcodes.Check:
                    {
                        var type = ReadConstant(fr) as SLType;
                        var checking = fr.Pop();
                        var checkingType = Type.FromValue(checking.Get(), Sl);
                        fr.Push(new SLBool(Type.Contains(type.Value, checkingType)));
                        break;
                    }
                    default:
                    {
                        throw new ArgumentOutOfRangeException(nameof(instruction), "Invalid opcode.");
                    }
                }
            }
            else
            {
                CurrentFrame = null;
            }
        }
        catch (UnhandledVmException e)
        {
            if (!Sunlite.Tick) throw;
            PrintStackTrace(e.Message);

        }
        catch (Exception e)
        {
            if (!Sunlite.Tick) throw;
            PrintStackTrace($"Internal VM Error: {e}");
        }
    }

    private static AnySLValue ReadConstant(CallFrame fr) => fr.Closure.Function.Chunk.Constants[ReadShort(fr)];
    
    private static SLString ReadString(CallFrame fr) => ReadConstant(fr) as SLString ?? new SLString("null");

    private static byte ReadByte(CallFrame fr) => fr.Closure.Function.Chunk.Code[fr.Pc++];

    private static short ReadShort(CallFrame fr)
    {
        fr.Pc += 2;
        var upByte = fr.Closure.Function.Chunk.Code[fr.Pc - 2];
        var lowByte = fr.Closure.Function.Chunk.Code[fr.Pc - 1];
        return (short)((upByte << 8) | lowByte);
    }
    
    private SLUpvalue?CaptureUpvalue(CallFrame fr, int index)
    {
        var value = fr.Stack.ElementAt(index);
        var found = OpenUpvalues.Find(up => up.closedValue == value);
        if (found is not null)
        {
            return found;
        }
        var up = new SLUpvalue(value);
        OpenUpvalues.Add(up);
        return up;
    }

    private static void DefineMethod(CallFrame fr, string name)
    {
        var method = (fr.Peek() as SLClosureObj)!;
        var clazz = (fr.Peek(1) as SLClassObj)!.Value;
        clazz.Methods[name] = method;
        fr.Pop();
    }
    
    private static void DefineField(CallFrame fr, string name)
    {
        var type = (fr.Peek(0) as SLType)!;
        var value = fr.Peek(1);
        var clazz = (fr.Peek(2) as SLClassObj)!.Value;
        clazz.FieldDefaults[name] = new SLField(type.Value, value);
        fr.Pop();
        fr.Pop();
    }

    private static void DefineStaticField(CallFrame fr, string name)
    {
        var type = (fr.Peek(0) as SLType)!;
        var value = fr.Peek(1);
        var clazz = (fr.Peek(2) as SLClassObj)!.Value;
        clazz.StaticFields[name] = new SLField(type.Value, value);
        fr.Pop();
        fr.Pop();
    }

    private static void DefineTypeParam(CallFrame fr, string name)
    {
        var type = (SLType)fr.Peek(0);
        var clazz = ((SLClassObj)fr.Peek(1)).Value;
        clazz.TypeParams[name] = type.Value;
        fr.Pop();
    }

    private static bool BindMethod(CallFrame fr, SLClass clazz, string name)
    {
        if (!clazz.Methods.TryGetValue(name, out var value)) return false;
        var bound = new SLBoundMethodObj(new SLBoundMethod(value.Value, fr.Peek(0)));
        fr.Pop();
        fr.Push(bound);
        return true;
    }

    private bool CallValue(AnySLValue callee, int argCount, int typeArgCount)
    {
        if (callee.IsObj())
        {
            switch (callee.Get())
            {
                case SLClosure closure:
                {
                    return Call(closure, argCount, typeArgCount);
                }
                case SLNativeFunction nativeFunction:
                {
                    return CallNative(nativeFunction, argCount, typeArgCount);
                }
                case SLClass clazz:
                {
                    if (clazz.IsAbstract)
                    {
                        RuntimeError($"Cannot instantiate abstract class '{clazz.Name}'.");
                        return false;
                    }
                    var stack = FrameStack.Peek().Stack;
                    Dictionary<string, SLField> fields = clazz.FieldDefaults.ToDictionary(kv => kv.Key, kv => kv.Value.Copy());
                    var instance = new SLClassInstanceObj(new SLClassInstance(clazz, [], fields));
                    stack[stack.Count - argCount - 1 - typeArgCount] = instance;
                    if (clazz.Methods.TryGetValue("init", out var initMethod))
                    {
                        List<Type> typeArgs = Enumerable.Range(0, typeArgCount)
                            .Select(i => (FrameStack.Peek().Pop() as SLType)!.Value)
                            .ToList();

                        for (var index = 0; index < typeArgs.Count; index++)
                        {
                            var typeArg = typeArgs[index];
                            instance.Value.TypeParams[clazz.TypeParams.Keys.ToList()[index]] = typeArg;
                        }
                        foreach (var field in fields.Values)
                        {
                            if (field.Type is not Type.Parameter typeParam) continue;
                            var name = typeParam.Name();
                            field.Type = instance.Value.TypeParams[name];
                        }

                        var success = Call(initMethod.Value, argCount, typeArgCount);
                        if(!success) return false;
                        FrameStack.Peek().Locals.Insert(0, instance);
                        return true;
                    } else if (argCount != 0)
                    {
                        RuntimeError($"Expected 0 arguments but got {argCount}.");
                        return false;
                    }
                    return true;
                }
                case SLBoundMethod method:
                {
                    if (method.Receiver is not SLClassInstanceObj && method.Receiver is not SLClassObj)
                    {
                        RuntimeError($"Invalid receiver '{method.Receiver}' for method '{method.Method.Function.Name}'.");
                        return false;
                    }

                    switch (method.Receiver)
                    {
                        case SLClassInstanceObj instance:
                        {
                            if (method.Method.Function.Modifier == FunctionModifier.Native)
                            {
                                var name = $"{instance.Value.Clazz.Name}#{method.Method.Function.Name}";
                                if (!Globals.TryGetValue(name, out var value))
                                {
                                    RuntimeError($"Native function '{name}' not bound to anything.");
                                    return false;
                                }

                                if (value is SLNativeFuncObj funcObj) return CallNative(funcObj.Value, argCount, typeArgCount);
                                RuntimeError($"Native function '{name}' bound to invalid value '{value}'.");
                                return false;
                            }
                            var success = Call(method.Method, argCount, typeArgCount);
                            if(!success) return false;
                            FrameStack.Peek().Locals.Insert(0, instance);
                            return true;
                        }
                        case SLClassObj clazz:
                        {
                            if (method.Method.Function.Modifier == FunctionModifier.StaticNative)
                            {
                                var name = $"{clazz.Value.Name}#{method.Method.Function.Name}";
                                if (!Globals.TryGetValue(name, out var value))
                                {
                                    RuntimeError($"Native function '{name}' not bound to anything.");
                                    return false;
                                }

                                if (value is SLNativeFuncObj funcObj) return CallNative(funcObj.Value, argCount, typeArgCount);
                                RuntimeError($"Native function '{name}' bound to invalid value '{value}'.");
                            }
                            else
                            {
                                RuntimeError("Can only call static methods on classes.");
                            }
                            return false;
                        }
                    }
                    break;
                }
            }
        }
        RuntimeError("Can only call functions.");
        return false;
    }

    public bool CallNative(SLNativeFunction nativeFunc, int argCount, int typeArgCount)
    {
        if(nativeFunc.Arity != -1 && nativeFunc.Arity != argCount)
        {
            RuntimeError($"Expected {nativeFunc.Arity} arguments but got {argCount}.");
            return false;
        }
        
        FrameStack.Peek().Stack.RemoveAt(FrameStack.Peek().Stack.Count - 1 - argCount);
        AnySLValue[] args = Enumerable.Range(0, argCount)
            .Select(_ => FrameStack.Peek().Pop())
            .Reverse()
            .ToArray();
        var value = nativeFunc.Call(this, args);
        FrameStack.Peek().Push(value);
        return true;
    }

    public bool Call(SLClosure closure, int argCount, int typeArgCount)
    {
        if (closure.Function.Modifier == FunctionModifier.Abstract)
        {
            RuntimeError($"Can't call abstract method '{closure.Function.Name}'.");
            return false;
        }

        if (argCount != closure.Function.Arity)
        {
            RuntimeError($"Expected {closure.Function.Arity} arguments but got {argCount}.");
            return false;
        }

        if (FrameStack.Count == MaxFrames)
        {
            RuntimeError("Stack overflow.");
            return false;
        }
        
        //LINQ destroys performance here
        
        /*List<AnySLValue> locals = Enumerable.Repeat<AnySLValue>(SLNil.Nil, closure.Function.LocalsCount - argCount).ToList();
        locals.AddRange(Enumerable.Range(0, argCount).Select(i => FrameStack.Peek().Peek(i)));
        locals.Reverse();*/
        
        var total = closure.Function.LocalsCount;
        var locals = new List<AnySLValue>(total);
        if (FrameStack.Count != 0)
        {
            var currentFrame = FrameStack.Peek();
            for (var i = argCount - 1; i >= 0; i--)
            {
                locals.Add(currentFrame.Peek(i));
            }
        }
        
        for (var i = 0; i < total - argCount; i++)
        {
            locals.Add(SLNil.Nil);
        }

        if(closure.Function.Modifier == FunctionModifier.Static) locals.Insert(0, SLNil.Nil);
        var frame = new CallFrame(closure, locals);
        FrameStack.Push(frame);
        return true;
    }

    public SLClosureObj? Load(string code)
    {

        if (Sl.Collector is null) return null;
        
        string path = "<loaded chunk>";

        var scanner = new Scanner(code, Sl);
        List<Token> tokens = scanner.ScanTokens(path);

        if (Sl.HadError)
        {
            Sl.HadError = false;
            return null;
        }
        
        var parser = new Parser(tokens, Sl);
        List<Stmt> statements = parser.Parse(path);
        
        if (Sl.HadError)
        {
            Sl.HadError = false;
            return null;
        }
        
        Sl.Collector.Collect(statements, path);
        
        if (Sl.HadError)
        {
            Sl.HadError = false;
            return null;
        }
        
        parser = new Parser(tokens, Sl);
        statements = parser.Parse(path);
        
        if (Sl.HadError)
        {
            Sl.HadError = false;
            return null;
        }

        var checker = new TypeChecker(Sl, this);
        checker.Check(statements, path);
        
        if (Sl.HadError)
        {
            Sl.HadError = false;
            return null;
        }

        var compiler = new Compiler(Sl, this);
        
        var program = compiler.Compile(FunctionType.None, FunctionModifier.Normal, Type.Nil, [], [], statements, path);
        
        // Stop if there was a compilation error.
        if (Sl.HadError)
        {
            Sl.HadError = false;
            return null;
        }

        return new SLClosureObj(new SLClosure(program, []));

    }

    private static bool IsFalse(AnySLValue value) => value is SLNil or SLBool { Value: false };
    
    public void ThrowException(int index, AnySLValue e)
    {
        if (index < 0)
        {
            throw new UnhandledVmException(e.ToString());
        }

        var fr = FrameStack.ElementAt(index);
        var closest = int.MaxValue;
        KeyValuePair<IntRange, IntRange>? exceptionHandler = null;
        foreach (var exception in fr.Closure.Function.Chunk.Exceptions)
        {
            if (!exception.Key.Contains(fr.Pc)) continue;
            var distance = exception.Value.Start - fr.Pc;
            if (closest <= distance) continue;
            exceptionHandler = exception;
            closest = distance;
        }

        if (exceptionHandler is not null)
        {
            Stack<CallFrame> stack = [];
            for (int i = 0; i < FrameStack.Count; i++)
            {
                var callFrame = FrameStack.ElementAt(i);
                if (i <= index) stack.Push(callFrame);
            }
            FrameStack.Clear();
            foreach (var frame in stack)
            {
                FrameStack.Push(frame);
            }

            if (fr != FrameStack.Peek())
            {
                throw new Exception($"Frames were unwinded incorrectly! {fr} != {FrameStack.Peek()}");
            }
            fr.Stack.Clear();
            fr.Locals[0] = e;
            fr.Pc = exceptionHandler.Value.Value.Start;
            if (CurrentException is not null) CurrentException = null;
        }
        else
        {
            if (CurrentException is null)
            {
                ExceptionStacktrace.Clear();
                ExceptionStacktrace.AddRange(FrameStack);
            }

            CurrentException = e;
            ThrowException(index - 1, e);
        }
    }
    
    public void PrintStackTrace(string e)
    {
        var fr = FrameStack.LastOrDefault();
        Sl.PrintErr(e);

        var sb = new StringBuilder();
        foreach (var frame in FrameStack)
        {
            sb.Append("\tat ");
            sb.Append(frame);
            sb.Append('\n');
        }

        sb.Append('\n');
        
        Sl.PrintErr(sb.ToString());
    }

    public void RuntimeError(string message)
    {
        Sl.HadRuntimeError = true;
        ThrowException(FrameStack.Count-1, new SLString(message));
    }
}