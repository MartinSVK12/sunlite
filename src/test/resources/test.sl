import HashMap from "/sunlite/stdlib/map";
import ArrayList from "/sunlite/stdlib/list";
import Iterator from "/sunlite/stdlib/iterable";

val map := HashMap(<String,Int>);
map.put("kek",420);
map.put("lol",69);
map.put("bruh",1337);
foreach(var (k, v) in map){
    print(k);
    print(v);
}
