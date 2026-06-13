import Reflect from "/sunlite/stdlib/reflect.sl";

class A {
    @Test
    static func test(){

    }
}

val arr: Array<String> = Reflect.getAnnotations(A.test);

//foreach (var annotation: String in arr) {
//    print(annotation);
//}


{
    val iter := Arrays.getIterator(arr);
    while (iter.hasNext() == true) {
        {
           var annotation := iter.current();
           {
                print(annotation);
            }
        }
        iter.next();
    }
}