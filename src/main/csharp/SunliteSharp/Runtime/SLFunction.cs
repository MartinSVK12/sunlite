using SunliteSharp.Core;
using SunliteSharp.Core.Modifier;
using Type = SunliteSharp.Core.Type;

namespace SunliteSharp.Runtime;

public record SLFunction(
    string Name,
    Type ReturnType,
    List<Param> Params,
    List<Param> TypeParams,
    Chunk Chunk,
    int Arity,
    int UpvalueCount,
    int LocalsCount,
    FunctionModifier Modifier
)
{
    public override string ToString()
    {
        return $"<function '{Name}({string.Join(", ",Params.Select( p => p.Type))}): {ReturnType}'>";
    }

    public SLFunction Copy()
    {
        return new SLFunction(Name, ReturnType, Params, TypeParams, Chunk, Arity, UpvalueCount, LocalsCount, Modifier);
    }
};