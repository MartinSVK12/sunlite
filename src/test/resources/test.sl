class A {
    var x := 0;

    func test(){
        print(x);
    }
}

val o: A? = nil;

o?.test() ?: print("elvis activated");