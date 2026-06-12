class A {

    var x := 1;

    func method() {
        print(this);
        print("hi");
    }
}

class B extends A {

    override func method() {
        print("hi 2");
    }

    func test() {
        super.method();
    }
}

B().test();