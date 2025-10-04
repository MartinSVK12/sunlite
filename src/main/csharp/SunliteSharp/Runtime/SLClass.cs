namespace SunliteSharp.Runtime;

using Type = SunliteSharp.Core.Type;

public record SLClass(
    string Name,
    Dictionary<string, SLClosureObj> Methods,
    Dictionary<string, SLField> FieldDefaults,
    Dictionary<string, SLField> StaticFields,
    Dictionary<string, Type> TypeParams,
    bool IsAbstract = false)
{
    public override string ToString()
    {
        return $"<class '{Name}'>";
    }
    
    public SLClass Copy()
    {
        return new SLClass(
            Name,
            Methods.ToDictionary(kv => kv.Key, kv => (SLClosureObj)kv.Value.Copy()),
            FieldDefaults.ToDictionary(kv => kv.Key, kv => kv.Value.Copy()),
            StaticFields.ToDictionary(kv => kv.Key, kv => kv.Value.Copy()),
            new Dictionary<string, Type>(TypeParams),
            IsAbstract
        );
    }
};