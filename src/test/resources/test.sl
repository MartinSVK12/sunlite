import Reflect from "/sunlite/stdlib/reflect.sl";

class A {
    static var sv := 69;
    var v := 420;

    static func sf() {
        print(sv);
    }

    func f() {
        print(v);
    }
}

val names: Array<Field> = Reflect.getFields(A());
array.forEach(names,func(o){print(o.toString());});