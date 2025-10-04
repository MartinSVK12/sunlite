namespace SunliteSharp.Runtime;

public record SLUpvalue(AnySLValue closedValue)
{
    public SLUpvalue Copy()
    {
        return new SLUpvalue(ClosedValue.Copy());
    }

    public AnySLValue ClosedValue { get; set; } = closedValue;
}