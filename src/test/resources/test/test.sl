import "/sunlite/stdlib/file";
import "/lox/scanner";
import "/sunlite/stdlib/list";

val file: File = File.open("test.lox");
val s: String = file.readText();

val scanner: Scanner = Scanner(s);

val list: List = scanner.scanTokens();
print(list.size());