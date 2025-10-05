using System.Diagnostics.CodeAnalysis;
using System.Text;
using SunliteSharp.Core.Enum;
using static SunliteSharp.Core.Enum.TokenType;

namespace SunliteSharp.Core.Compiler;

[SuppressMessage("ReSharper", "InconsistentNaming")]
public class Scanner(string source, Sunlite sunlite)
{
    private string CurrentFile = "";
    
    private readonly List<Token> Tokens = [];

    private int Start = 0;
    private int Current = 0;
    private int Line = 1;
    private int TotalChars = 0;
    private int LineStart = 0;
    private int LineCurrent = 0;

    private static readonly Dictionary<string, TokenType> Keywords = new()
    {
        { "import", Import },
        { "and", And },
        { "or", Or },
        { "true", True },
        { "false", False },
        { "nil", Nil },
        { "if", If },
        { "else", Else },
        { "for", For },
        { "while", While },
        { "break", Break },
        { "continue", Continue },
        { "try", Try },
        { "catch", Catch },
        { "throw", Throw },
        { "fun", Fun },
        { "class", Class },
        { "interface", Interface },
        { "extends", Extends },
        { "implements", Implements },
        { "init", Init },
        { "this", This },
        { "super", Super },
        { "return", Return },
        { "var", Var },
        { "val", Val },
        { "static", Static },
        { "native", Native },
        { "is", Is },
        { "isnt", IsNot },
        { "as", As },
        { "Any", TypeAny },
        { "Boolean", TypeBoolean },
        { "Number", TypeNumber },
        { "String", TypeString },
        { "Array", TypeArray },
        { "Table", TypeTable },
        { "Generic", TypeGeneric },
        { "Class", TypeClass },
        { "Function", TypeFunction },
        { "Nil", TypeNil },
    };

    public List<Token> ScanTokens(string path)
    {
        CurrentFile = path;
        
        while (!IsAtEnd()) {
            // We are at the beginning of the next lexeme.
            Start = Current;
            LineStart = LineCurrent;
            ScanToken();
        }
        
        Tokens.Add(new Token(Eof, "", null, Line, CurrentFile, new Token.Position(0, 0)));
        return Tokens;
    }

    private bool IsAtEnd()
    {
        return Current >= source.Length;
    }
    
    private void ScanToken()
    {
        var c = Advance();
        switch (c)
        {
            case '(': AddToken(LeftParen); break;
            case ')': AddToken(RightParen); break;
            case '{': AddToken(LeftBrace); break;
            case '}': AddToken(RightBrace); break;
            case '[': AddToken(LeftBracket); break;
            case ']': AddToken(RightBracket); break;
            case ',': AddToken(Comma); break;
            case '.': AddToken(Dot); break;
            case '-': AddToken(Match('=') ? MinusEqual : Minus); break;
            case '+': AddToken(Match('=') ? PlusEqual : Plus); break;
            case ';': AddToken(Semicolon); break;
            case '*': AddToken(Star); break;
            case ':': AddToken(Colon); break;
            case '|': AddToken(Pipe); break;
            case '?': AddToken(Question); break;
            case '!': AddToken(Match('=') ? BangEqual : Bang); break;
            case '=': AddToken(Match('=') ? EqualEqual : Equal); break;
            case '<': AddToken(Match('=') ? LessEqual : Less); break;
            case '>': AddToken(Match('=') ? GreaterEqual : Greater); break;
            case '/':
                if (Match('/'))
                {
                    while (Peek() != '\n' && !IsAtEnd()) Advance();
                }
                else if (Match('*'))
                {
                    while (Peek() != '*' && PeekNext() != '/' && !IsAtEnd()) Advance();
                    if (!IsAtEnd() && Peek() == '*' && PeekNext() == '/')
                    {
                        Advance();
                        Advance();
                    }
                }
                else
                {
                    AddToken(Slash);
                }
                break;
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n':
                LineStart = 0;
                LineCurrent = 0;
                Line++;
                break;
            case '"':
                String();
                break;
            default:
                if (char.IsDigit(c))
                {
                    Number();
                }
                else if (char.IsAsciiLetter(c) || c == '_')
                {
                    Identifier();
                }
                else
                {
                    sunlite.Error($"Unexpected character '{c}'", Line, CurrentFile);
                }
                break;
        }
    }

    private void AddToken(TokenType type, object? literal = null) 
    {
        var text = source.Substring(Start, Current - Start);
        Tokens.Add(new Token(type, text, literal, Line, CurrentFile, new Token.Position(LineStart, LineCurrent)));
    }

    private char Advance()
    {
        TotalChars++;
        LineCurrent++;
        return source[Current++];
    }

    private char Peek()
    {
        return IsAtEnd() ? '\u0000' : source[Current];
    }
    
    private char PeekNext()
    {
        return Current + 1 >= source.Length ? '\u0000' : source[Current + 1];
    }

    private bool Match(char expected)
    {
        if (IsAtEnd()) return false;
        if (source[Current] != expected) return false;
        Current++;
        TotalChars++;
        LineCurrent++;
        return true;
    }

    private void String()
    {
        var sb = new StringBuilder();
        while (Peek() != '"' && !IsAtEnd())
        {
            if(Peek() == '\n') Line++;
            
            //escape sequences
            if (Peek() == '\\')
            {
                Advance();
                var ch = Peek();
                char formatChar = ' ';
                switch (ch)
                {
                    case 'n':
                        formatChar = '\n';
                        break;
                    case '\\' :
                        formatChar = '\\';
                        break;
                    case '"':
                        formatChar = '"';
                        break;
                    default:
                        sunlite.Error("Unknown escape sequence.", Line, CurrentFile);
                        break;
                }
                Advance();
                sb.Append(formatChar);
            }

            //interpolation
            if (Peek() == '$' && PeekNext() == '{')
            {
                AddToken(TokenType.String, sb.ToString());
                Advance();
                Advance();
                
                Tokens.Add(new Token(Plus, "+", null, Line, CurrentFile, new Token.Position(LineStart, LineCurrent)));
                
                var iSb = new StringBuilder();
                while (Peek() != '}' && !IsAtEnd())
                {
                    iSb.Append(Peek());
                    Advance();
                }
                
                var innerScanner = new Scanner(iSb.ToString(), sunlite);
                List<Token> interpolatedTokens = innerScanner.ScanTokens(CurrentFile);
                interpolatedTokens = interpolatedTokens[..^1];
                Console.WriteLine(interpolatedTokens);
                
                Tokens.AddRange(interpolatedTokens);
                
                Tokens.Add(new Token(Plus, "+", null, Line, CurrentFile, new Token.Position(LineStart, LineCurrent)));

                if (IsAtEnd())
                {
                    sunlite.Error("Expected '}' after string interpolated expression.", Line, CurrentFile);
                    return;
                }
                Advance();
                sb.Clear();
            }

            sb.Append(Peek());
            Advance();
        }

        if (IsAtEnd())
        {
            sunlite.Error("Unterminated string.", Line, CurrentFile);
        }

        Advance();
        
        AddToken(TokenType.String, sb.ToString());
    }

    private void Identifier()
    {
        while (char.IsLetterOrDigit(Peek()) || Peek() == '_')
        {
            Advance();
        }
        
        var text = source.Substring(Start, Current - Start);
        var type = Keywords.GetValueOrDefault(text, TokenType.Identifier);
        AddToken(type);
    }

    private void Number()
    {
        while (char.IsDigit(Peek()))
        {
            Advance();
        }

        if (Peek() == '.' && char.IsDigit(PeekNext()))
        {
            Advance();
            while (char.IsDigit(Peek()))
            {
                Advance();
            }
        }
        
        AddToken(TokenType.Number, double.Parse(source.Substring(Start, Current - Start)));
    }
}