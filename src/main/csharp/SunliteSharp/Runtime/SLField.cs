namespace SunliteSharp.Runtime;

using Type = SunliteSharp.Core.Type;

public record SLField(Type Type, AnySLValue Value)
{
    public SLField Copy()
    {
        return this with { Value = Value.Copy() };
    }

    public Type Type { get; set; } = Type;
    public AnySLValue Value { get; set; } = Value;
}