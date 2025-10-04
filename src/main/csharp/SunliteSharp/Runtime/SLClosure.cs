namespace SunliteSharp.Runtime;

public record SLClosure(SLFunction Function, SLUpvalue?[] Upvalues)
{
    public override string ToString()
    {
        return Function.ToString();
    }

    public SLClosure Copy()
    {
        return this with { Upvalues = Upvalues.Select(u => u?.Copy()).ToArray() };
    }
}