import "/sunlite/stdlib/file.sl";
import "/lox/scanner.sl";
import "/sunlite/stdlib/list.sl";

val file: File = File.open("test.lox");
val s: String = file.readText();

val scanner: Scanner = Scanner(s);

val list: List = scanner.scanTokens();
print(list.size());