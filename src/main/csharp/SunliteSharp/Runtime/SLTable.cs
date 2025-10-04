namespace SunliteSharp.Runtime;

public class SLTable(Sunlite sl)
{
    public AnySLValue this[AnySLValue key]
    {
        get => Internal[key];
        set => Internal[key] = value;
    }
    
    public Dictionary<AnySLValue, AnySLValue> Internal { get; private set; } = new();

    public SLTable Overwrite(Dictionary<AnySLValue, AnySLValue> m)
    {
        Internal = m;
        return this;
    }
    
    public SLTable Copy()
    {
        return new SLTable(sl).Overwrite(Internal.ToDictionary(kv => kv.Key.Copy(), kv => kv.Value.Copy()));
    }

    public override string ToString()
    {
        return $"<table of size {Internal.Count}>";
    }
}