import <"/sunlite/stdlib/list.sl">;

val l := List(<String>);
val l2 := List(<String>);

l.add("lol");
l.add("yeet");

l2.add("lmao");

l2.addAll(l);

l2.forEach(func(o: String){ print(o); });