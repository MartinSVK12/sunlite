namespace SunliteSharp.Runtime;

using System.Collections.Generic;
using System.Linq;
using Type = SunliteSharp.Core.Type;

public record SLClassInstance(
    SLClass Clazz,
    Dictionary<string, Type> TypeParams,
    Dictionary<string, SLField> Fields
)
{
    public override string ToString() => $"<object '{Clazz.Name}'>";

    public SLClassInstance Copy()
    {
        return new SLClassInstance(
            Clazz.Copy(),
            new Dictionary<string, Type>(TypeParams),
            Fields.ToDictionary(kv => kv.Key, kv => kv.Value.Copy())
        );
    }
        
}