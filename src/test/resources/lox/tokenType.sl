class TokenType {
    // One-character tokens (static values are the ASCII codes)
    static val LEFT_PAREN: Int = 40;
    static val RIGHT_PAREN: Int = 41;
    static val LEFT_BRACE: Int = 123;
    static val RIGHT_BRACE: Int = 125;
    static val COMMA: Int = 44;
    static val DOT: Int = 46;
    static val MINUS: Int = 45;
    static val PLUS: Int = 43;
    static val SEMICOLON: Int = 59;
    static val SLASH: Int = 47;
    static val STAR: Int = 42;
    static val BANG: Int = 33;
    static val EQUAL: Int = 61;
    static val GREATER: Int = 62;
    static val LESS: Int = 60;

    // Two-character tokens
    static val BANG_EQUAL: Int = 256;
    static val EQUAL_EQUAL: Int = 257;
    static val GREATER_EQUAL: Int = 258;
    static val LESS_EQUAL: Int = 259;

    // Literals
    static val IDENTIFIER: Int = 260;
    static val STRING: Int = 261;
    static val NUMBER: Int = 262;

    // Keywords
    static val AND: Int = 263;
    static val CLASS: Int = 264;
    static val ELSE: Int = 265;
    static val FALSE: Int = 266;
    static val FUN: Int = 267;
    static val FOR: Int = 268;
    static val IF: Int = 269;
    static val NIL: Int = 270;
    static val OR: Int = 271;
    static val PRINT: Int = 272;
    static val RETURN: Int = 273;
    static val SUPER: Int = 274;
    static val THIS: Int = 275;
    static val TRUE: Int = 276;
    static val VAR: Int = 277;
    static val WHILE: Int = 278;

    // Misc tokens
    static val EOF: Int = 279;
    static val INVALID: Int = 280;

    static func toStr(type): String? {
      if (type == TokenType.LEFT_PAREN) return "(";
      if (type == TokenType.RIGHT_PAREN) return ")";
      if (type == TokenType.LEFT_BRACE) return "{";
      if (type == TokenType.RIGHT_BRACE) return "}";
      if (type == TokenType.COMMA) return ",";
      if (type == TokenType.DOT) return ".";
      if (type == TokenType.MINUS) return "-";
      if (type == TokenType.PLUS) return "+";
      if (type == TokenType.SEMICOLON) return ";";
      if (type == TokenType.SLASH) return "/";
      if (type == TokenType.STAR) return "*";
      if (type == TokenType.BANG) return "!";
      if (type == TokenType.EQUAL) return "=";
      if (type == TokenType.GREATER) return ">";
      if (type == TokenType.LESS) return "<";

      if (type == TokenType.BANG_EQUAL) return "!=";
      if (type == TokenType.EQUAL_EQUAL) return "==";
      if (type == TokenType.GREATER_EQUAL) return ">=";
      if (type == TokenType.LESS_EQUAL) return "<=";

      if (type == TokenType.IDENTIFIER) return "<identifier>";
      if (type == TokenType.STRING) return "<string>";
      if (type == TokenType.NUMBER) return "<number>";

      if (type == TokenType.AND) return "and";
      if (type == TokenType.CLASS) return "class";
      if (type == TokenType.ELSE) return "else";
      if (type == TokenType.FALSE) return "false";
      if (type == TokenType.FUN) return "fun";
      if (type == TokenType.FOR) return "for";
      if (type == TokenType.IF) return "if";
      if (type == TokenType.NIL) return "nil";
      if (type == TokenType.OR) return "or";
      if (type == TokenType.PRINT) return "print";
      if (type == TokenType.RETURN) return "return";
      if (type == TokenType.SUPER) return "super";
      if (type == TokenType.THIS) return "this";
      if (type == TokenType.TRUE) return "true";
      if (type == TokenType.VAR) return "var";
      if (type == TokenType.WHILE) return "while";

      if (type == TokenType.EOF) return "<eof>";
      if (type == TokenType.INVALID) return "<invalid>";

      return nil;
    }

}