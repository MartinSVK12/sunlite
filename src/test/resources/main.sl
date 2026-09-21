import File from "/sunlite/stdlib/file";
import Scanner from "/lox/scanner";
import List from "/sunlite/stdlib/list";

val file: File = File("test.lox").open();
val s: String = file.readText();

val scanner: Scanner = Scanner(s);

val list: List<Token> = scanner.scanTokens();
list.forEach(func(o: Token){ print(o.toString()); });