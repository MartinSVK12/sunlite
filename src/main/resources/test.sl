import "/reflect.sl";
import "/object.sl";
import "/array.sl";

class A extends object {
    static fun method(a: Number, b: String){
        print(a);
        print(b);
    }
}

//A.method(2, "hi");
var l: Function<Any?, Nil> = fun(o: Any?){ print(o); };
var arr: Array = (reflect.getMethods(A) as Array);
print(arr);
arr.forEach(l);