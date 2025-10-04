namespace SunliteSharp.Runtime;

using Type = SunliteSharp.Core.Type;

public record SLNativeFunction(string Name, Type ReturnType, int Arity, Func<VM, AnySLValue[], AnySLValue> Call)
{
    public override string ToString()
    {
        return $"<native fn '{Name}'>";
    }
}