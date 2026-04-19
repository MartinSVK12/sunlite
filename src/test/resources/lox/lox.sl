class Lox {
    static var hadError: Boolean = false;
    static var hadRuntimeError: Boolean = false;
    
    static func error(line: Int, message: String) {
        Lox.report(line, "", message);
    }
    
    static func report(line: Int, where: String, message: String){
        print("[line ${str(line)}] Error${where}: ${message}.");
        Lox.hadError = true;
    }
}
