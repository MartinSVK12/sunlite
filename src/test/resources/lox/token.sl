import TokenType from "/lox/tokenType.sl";

class Token {
    var type: Int = 0;
    var lexeme: String = "";
    var literal: Any? = nil;
    var line: Int = 0;
    
    init(t: Int, x: String, l: Any?, i: Int){
        this.type = t;
        this.lexeme = x;
        this.literal = l;
        this.line = i;
    }
    
    func toString(): String {
        return "<token '" + TokenType.toStr(this.type) + " '" + string.trim(this.lexeme) + "' " + str(this.literal) + "'>";
    }

}