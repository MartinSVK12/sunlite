import List from "/sunlite/stdlib/list.sl";
import ArrayList from "/sunlite/stdlib/list.sl";
import ListIterator from "/sunlite/stdlib/list.sl";

val list: List<Int> = ArrayList(<Int>);

list.add(69);
list.add(420);
list.add(1337);

list.forEach(func(o: Int){ print(o); });

foreach(var o: Int in list){
    print(o);
}

if(list.any(func(o: Int): Boolean { return o == 69; })){
    print("yeet");
}

if(list.none(func(o: Int): Boolean { return o == 666; })){
    print("the devils not here");
}

if(list.all(func(o: Int): Boolean { return o == 1; })){
    print("all in one");
}