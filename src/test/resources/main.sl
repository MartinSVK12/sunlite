import File from "/sunlite/stdlib/file.sl";
import Scanner from "/lox/scanner.sl";
import List from "/sunlite/stdlib/list.sl";

val file: File = File.open("test.lox");
val s: String = file.readText();

val scanner: Scanner = Scanner(s);

val list: List<Token> = scanner.scanTokens();
list.forEach(func(o: Token){ print(o.toString()); });