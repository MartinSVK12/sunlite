import Field from "/sunlite/stdlib/reflect.sl";

class Reflect {
    static native func getMethodNames(c: Class | Object): Array<String>
    static native func getFieldNames(c: Class | Object): Array<String>
    static native func getAnnotations(f: Function): Array<String>

    static func getFields(c: Class | Object): Array<Field> {
        var names: Array<String> = Reflect.getFieldNames(c);
        var arr: Array<Field> = emptyArray(<Field>sizeOf(names));
        for (var i: Int = 0; i < sizeOf(names); i++) {
            arr[i] = Field(c, names[i]);
        }
        return arr;
    }
}

class Field {

    var clazz: Class | Object;
    var name: String;

    init(c: Class | Object, n: String) {
        this.clazz = c;
        this.name = n;
        loadField();
    }

    native func loadField()
    native func<T> get(): Generic<T>?
    native func set(v: Any?)

    override func toString(): String {
        return "<field '${name}'>";
    }
}