class A {
    var x := 0;

    func test(): Int {
        print(x);
        return x;
    }
}

val o: A? = A();

o?.test() ?: print("elvis activated");