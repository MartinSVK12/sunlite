namespace SunliteSharp.Core.Enum;

public enum TokenGroup
{
    SingleChar,
    MultiChar,
    Literals,
    Keywords,
    Types,
    End
}

public enum TokenType
{
    // Single-character tokens.
    LeftParen,
    RightParen,
    LeftBrace,
    RightBrace,
    LeftBracket,
    RightBracket,
    Comma,
    Dot,
    Minus,
    Plus,
    Semicolon,
    Slash,
    Star,
    Colon,
    Pipe,
    Question,

    // One or two character tokens.
    Bang,
    BangEqual,
    Equal,
    EqualEqual,
    Greater,
    GreaterEqual,
    Less,
    LessEqual,
    PlusEqual,
    MinusEqual,
    // INCREMENT, DECREMENT,

    // Literals.
    Identifier,
    String,
    Number,

    // Keywords.
    And,
    Class,
    Else,
    False,
    Fun,
    Init,
    For,
    If,
    Nil,
    Or,
    Print,
    Return,
    Super,
    This,
    True,
    Var,
    Val,
    While,
    Break,
    Continue,
    Static,
    Interface,
    Is,
    IsNot,
    Import,
    Native,
    Dynamic,
    As,
    Extends,
    Implements,
    Try,
    Catch,
    Throw,

    // Type keywords
    TypeString,
    TypeNumber,
    TypeBoolean,
    TypeFunction,
    TypeAny,
    TypeArray,
    TypeGeneric,
    TypeClass,
    TypeNil,
    TypeTable,

    Eof
}

public static class TokenTypeExtensions
{
    public static TokenGroup GetGroup(this TokenType type)
    {
        switch (type)
        {
            // Single-character tokens.
            case TokenType.LeftParen:
            case TokenType.RightParen:
            case TokenType.LeftBrace:
            case TokenType.RightBrace:
            case TokenType.LeftBracket:
            case TokenType.RightBracket:
            case TokenType.Comma:
            case TokenType.Dot:
            case TokenType.Minus:
            case TokenType.Plus:
            case TokenType.Semicolon:
            case TokenType.Slash:
            case TokenType.Star:
            case TokenType.Colon:
            case TokenType.Pipe:
            case TokenType.Question:
                return TokenGroup.SingleChar;

            // One or two character tokens.
            case TokenType.Bang:
            case TokenType.BangEqual:
            case TokenType.Equal:
            case TokenType.EqualEqual:
            case TokenType.Greater:
            case TokenType.GreaterEqual:
            case TokenType.Less:
            case TokenType.LessEqual:
            case TokenType.PlusEqual:
            case TokenType.MinusEqual:
                return TokenGroup.MultiChar;

            // Literals.
            case TokenType.Identifier:
            case TokenType.String:
            case TokenType.Number:
                return TokenGroup.Literals;

            // Keywords.
            case TokenType.And:
            case TokenType.Class:
            case TokenType.Else:
            case TokenType.False:
            case TokenType.Fun:
            case TokenType.Init:
            case TokenType.For:
            case TokenType.If:
            case TokenType.Nil:
            case TokenType.Or:
            case TokenType.Print:
            case TokenType.Return:
            case TokenType.Super:
            case TokenType.This:
            case TokenType.True:
            case TokenType.Var:
            case TokenType.Val:
            case TokenType.While:
            case TokenType.Break:
            case TokenType.Continue:
            case TokenType.Static:
            case TokenType.Interface:
            case TokenType.Is:
            case TokenType.IsNot:
            case TokenType.Import:
            case TokenType.Native:
            case TokenType.Dynamic:
            case TokenType.As:
            case TokenType.Extends:
            case TokenType.Implements:
            case TokenType.Try:
            case TokenType.Catch:
            case TokenType.Throw:
                return TokenGroup.Keywords;

            // Type keywords
            case TokenType.TypeString:
            case TokenType.TypeNumber:
            case TokenType.TypeBoolean:
            case TokenType.TypeFunction:
            case TokenType.TypeAny:
            case TokenType.TypeArray:
            case TokenType.TypeGeneric:
            case TokenType.TypeClass:
            case TokenType.TypeNil:
            case TokenType.TypeTable:
                return TokenGroup.Types;

            case TokenType.Eof:
            default:
                return TokenGroup.End;
        }
    }
}