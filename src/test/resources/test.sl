class A {
    var x: Int;

    init(x: Int) {
        this.x = x;
    }
}

class B extends A {
    var y: Int;

    init(x: Int, y: Int) {
        super(x);
        this.y = y;
    }

    override func toString(): String {
        return "B(${str(this.x)}, ${str(this.y)})";
    }
}

print(B(1,2).toString());