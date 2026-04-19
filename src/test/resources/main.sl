import "/sunlite/stdlib/file.sl";
import "/lox/scanner.sl";
import "/sunlite/stdlib/list.sl";

val file: File = File.open("test.lox");
val s: String = file.readText();

val scanner: Scanner = Scanner(s);

val list: List<Token> = scanner.scanTokens();
list.forEach(func(o: Token){ print(o.toString()); } as Function<Generic<T>,Nil>);