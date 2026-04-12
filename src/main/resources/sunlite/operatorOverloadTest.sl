import "/object.sl";

class Vec2 extends Object {
    var x: Int = 0;
    var y: Int = 0;
    
    init(x: Int, y: Int){
        this.x = x;
        this.y = y;
    }
    
    operator func add(other: Any?): Vec2 {
        return Vec2(this.x + other.x, this.y + other.y);
    }
    
    operator func equals(other: Any?): Boolean {
        return this.x == other.x and this.y == other.y;
    }
    
    override func toString(): String {
        return "(${str(this.x)}, ${str(this.y)})";
    }
}

val a: Vec2 = Vec2(1,2);
val b: Vec2 = Vec2(3,1);
print((a + b).toString());