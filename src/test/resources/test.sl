include <"/sunlite/stdlib/list.sl">;

val list := List(<Int>);

for(var i = 0; i < 100; i++){
    list.add(rand(100));
}

list.forEach(func(o: Int){ print(o); });

val filtered: List<Int> = list.filter(func(o: Int): Boolean { return o > 50; });

filtered.forEach(func(o: Int){ print(o); });