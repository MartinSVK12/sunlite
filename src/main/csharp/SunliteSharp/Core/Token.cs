using SunliteSharp.Core.Enum;

namespace SunliteSharp.Core;

public readonly record struct Token(TokenType Type, string Lexeme, object? Literal, int Line, string File, Token.Position Pos)
{
    public readonly record struct Position(int Start, int End);

    public static Token Identifier(string name, string file)
    {
        return new Token(TokenType.Identifier, name, null, -1, file, new Position(-1, -1));
    }
    
    public static Token Identifier(string name, int line, string file)
    {
        return new Token(TokenType.Identifier, name, null, line, file, new Position(-1, -1));
    }
}