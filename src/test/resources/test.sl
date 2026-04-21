include <"/sunlite/stdlib/list.sl">;

val list := List(<Int>);

for(var i = 0; i < 10; i++){
    list.add(rand(100));
}

print("raw");

list.forEach(func(o: Int){ print(o); });

val filtered := list.filter(func(o: Int): Boolean { return o > 50; });

print("filtered");

filtered.forEach(func(o: Int){ print(o); });

val mapped := filtered.map(<String> func(o: Int): String { return "s" + str(o); });

print("mapped");

mapped.forEach(func(o: String){ print(o); });