using SunliteSharp.Core.Enum;

namespace SunliteSharp.Core.Modifier;

public enum FunctionModifier
{
    Normal,
    Init,
    Static,
    Abstract,
    Native,
    StaticNative,
}

public static class FunctionModifierExtensions
{
    public static FunctionModifier Get(Token? token, Token? token2 = null)
    {
        return token?.Type switch
        {
            TokenType.Static when token2?.Type == TokenType.Native => FunctionModifier.StaticNative,
            TokenType.Static => FunctionModifier.Static,
            TokenType.Native => FunctionModifier.Native,
            _ => FunctionModifier.Normal
        };
    }
}