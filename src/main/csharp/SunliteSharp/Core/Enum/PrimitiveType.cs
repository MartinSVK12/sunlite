namespace SunliteSharp.Core.Enum;

public enum PrimitiveType
{
    Any,
    Number,
    String,
    Boolean,
    Function,
    Class,
    Object,
    Array,
    Table,
    Generic,
    Nil,
    Unknown
}

public static class PrimitiveTypeExtensions
{
    public static PrimitiveType Get(this Token? token)
    {
        return token?.Type switch
        {
            TokenType.TypeAny => PrimitiveType.Any,
            TokenType.TypeNumber => PrimitiveType.Number,
            TokenType.TypeString => PrimitiveType.String,
            TokenType.TypeBoolean => PrimitiveType.Boolean,
            TokenType.TypeFunction => PrimitiveType.Function,
            TokenType.TypeClass => PrimitiveType.Class,
            TokenType.Identifier => PrimitiveType.Object,
            TokenType.TypeArray => PrimitiveType.Array,
            TokenType.TypeTable => PrimitiveType.Table,
            TokenType.TypeGeneric => PrimitiveType.Generic,
            TokenType.TypeNil => PrimitiveType.Nil,
            TokenType.Question => PrimitiveType.Nil,
            _ => PrimitiveType.Unknown
        };
    }
}