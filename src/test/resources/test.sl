class A {
    required func test(){
        print("hello world");
    }
}

class B extends A {
    override func test(){
        super.test();
        print("goodbye world");
    }
}

A().test();
B().test();