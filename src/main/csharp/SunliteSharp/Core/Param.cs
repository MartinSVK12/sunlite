namespace SunliteSharp.Core;

public record struct Param(Token Token, Type Type)
{
    public override string ToString()
    {
        return $"{Token.Lexeme}: {Type.Name()}";
    }
}