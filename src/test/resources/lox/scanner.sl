import TokenType from "/lox/tokenType.sl";
import Token from "/lox/token.sl";
import Lox from "/lox/lox.sl";
import List from "/sunlite/stdlib/list.sl";
import ArrayList from "/sunlite/stdlib/list.sl";

class Scanner {
    var source: String = "";
    var tokens: List<Token> = ArrayList(<Token>);

    static val KEYWORDS: Table<String, Int> = emptyTable(<String, Int>);

    static init {
        Scanner.KEYWORDS["and"] = TokenType.AND;
        Scanner.KEYWORDS["class"] = TokenType.CLASS;
        Scanner.KEYWORDS["else"] = TokenType.ELSE;
        Scanner.KEYWORDS["false"] = TokenType.FALSE;
        Scanner.KEYWORDS["for"] = TokenType.FOR;
        Scanner.KEYWORDS["fun"] = TokenType.FUN;
        Scanner.KEYWORDS["if"] = TokenType.IF;
        Scanner.KEYWORDS["nil"] = TokenType.NIL;
        Scanner.KEYWORDS["or"] = TokenType.OR;
        Scanner.KEYWORDS["print"] = TokenType.PRINT;
        Scanner.KEYWORDS["return"] = TokenType.RETURN;
        Scanner.KEYWORDS["super"] = TokenType.SUPER;
        Scanner.KEYWORDS["this"] = TokenType.THIS;
        Scanner.KEYWORDS["true"] = TokenType.TRUE;
        Scanner.KEYWORDS["var"] = TokenType.VAR;
        Scanner.KEYWORDS["while"] = TokenType.WHILE;
    }
    
    var start: Int = 0;
    var current: Int = 0;
    var line: Int = 1;
    
    init(s: String){
        source = s;
    }
    
    func scanTokens(): List<Token> {
        while(!isAtEnd()){
            start = current;
            scanToken();
        }
        
        tokens.add(Token(TokenType.EOF, "", nil, line));
        return tokens;
    }
    
    func scanToken() {
        val c: String = advance();
        match(c) {
            "(": { addToken(TokenType.LEFT_PAREN); }
            ")": { addToken(TokenType.RIGHT_PAREN); }
            "{": { addToken(TokenType.LEFT_BRACE); }
            "}": { addToken(TokenType.RIGHT_BRACE); }
            ",": { addToken(TokenType.COMMA); }
            ".": { addToken(TokenType.DOT); }
            "-": { addToken(TokenType.MINUS); }
            "+": { addToken(TokenType.PLUS); }
            ";": { addToken(TokenType.SEMICOLON); }
            "*": { addToken(TokenType.STAR); }
            "!": {
                if(matches("=")){
                    addToken(TokenType.BANG_EQUAL);
                } else {
                    addToken(TokenType.BANG);
                }
            }
            "=": {
                if(matches("=")){
                    addToken(TokenType.EQUAL_EQUAL);
                } else {
                    addToken(TokenType.EQUAL);
                }
            }
            "<": {
                if(matches("=")){
                    addToken(TokenType.LESS_EQUAL);
                } else {
                    addToken(TokenType.LESS);
                }
            }
            ">": {
                if(matches("=")){
                    addToken(TokenType.GREATER_EQUAL);
                } else {
                    addToken(TokenType.GREATER);
                }
            }
            "/": {
                if(matches("/")){
                    while(peek() != "\n" and !isAtEnd()){
                        advance();
                    }
                } else {
                    addToken(TokenType.SLASH);
                }
            }
            " ", "\r", "\t": {
                
            }
            "\n": {
                line = line + 1;
            }
            "\"": {
                strng();
            }
            else: {
                if(isDigit(c)){
                    number();
                } else if(isAlpha(c)){
                    identifier();
                } else {
                    Lox.error(line, "Unexpected character.");
                }
            }
        }
    }
    
    func strng(){
        while(peek() != "\"" and !isAtEnd()){
            if(peek() == "\n") {
                line = line + 1;
            }
            advance();
        }
        
        if(isAtEnd()){
            Lox.error(line, "Unterminated string.");
            return;
        }
        
        advance();
        val s: String = source.sub(start + 1, current - 1);
        addTokenWithValue(TokenType.STRING, s);
    }
    
    func number(){
        while(isDigit(peek())) {
            advance();
        }
        
        if(peek() == "." and isDigit(peekNext())){
            advance();
            
            while(isDigit(peek())) {
                advance();
            }
        }
        
        val s: String = string.sub(source, start, current+1);
        addTokenWithValue(TokenType.NUMBER, parseDouble(s));
    }
    
    func identifier(){
        while(isAlphaNumeric(peek())) {
            advance();
        }
        val s: String = source.sub(start, current);
        var type: Int = Scanner.KEYWORDS[s];
        if(type == nil){
           type = TokenType.IDENTIFIER; 
        }
        addToken(type);
    }
    
    func peek(): String {
        if(isAtEnd()) return "";
        return source[current];
    }
    
    func peekNext(): String {
        if(current + 1 >= source.len()) return "";
        return source[current+1];
    }
    
    func matches(expected: String): Boolean {
        if(isAtEnd()) return false;
        if(source[current] != expected) return false;
        current = current + 1;
        return true;
    }
    
    func advance(): String {
        val s: String = source[current];
        current = current + 1;
        return s;
    }
    
    func isAtEnd(): Boolean {
        val b: Boolean = current >= source.len()-1;
        return b;
    }
    
    func isDigit(c: String): Boolean {
        return c >= "0" and c <= "9";
    }
    
    func isAlpha(c: String): Boolean {
         return (c >= "a" and c <= "z") or
           (c >= "A" and c <= "Z") or
            c == "_";
    }
    
    func isAlphaNumeric(c: String): Boolean {
        return isAlpha(c) or isDigit(c);
    }
    
    func addToken(type: Int){
        addTokenWithValue(type, nil);
    }
    
    func addTokenWithValue(type: Int, literal: Any?) {
        val text: String = source.sub(start, current);
        tokens.add(Token(type, text, literal, line));
    }
}