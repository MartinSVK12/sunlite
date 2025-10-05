namespace SunliteSharp.Runtime;

public record CallFrame(SLClosure Closure, List<AnySLValue> Locals)
{
    public int Pc = 0;
    public List<AnySLValue> Stack = [];

    public AnySLValue Pop()
    {
        var value = Stack[^1];
        Stack.RemoveAt(Stack.Count - 1);
        return value;
    }
    
    public void Push(AnySLValue value)
    {
        Stack.Add(value);
    }

    public AnySLValue Peek()
    {
        return Stack[^1];
    }

    public AnySLValue Peek(int offset)
    {
        return Stack[Stack.Count - offset - 1];
    }

    public override string ToString()
    {
        return $"[line {Closure.Function.Chunk.Header.Lines[Pc]}] in {(Closure.Function.Name == "" ? $"file {Closure.Function.Chunk.Header.File}" : $"function {Closure.Function.Name}")}";
    }
}