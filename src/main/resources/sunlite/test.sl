class A {
    
    var v: Int = 42;

    func g(){
        var v: Int = 24;
        print(v);
    }

}

class B extends A {
    func f() {
        g();
    }
}
B().f();