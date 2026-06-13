import Reflect from "/sunlite/stdlib/reflect.sl";

class A {
    @Test
    static native func test()
}

val arr: Array<String> = Reflect.getAnnotations(A.test);

foreach (var annotation: String in arr) {
    print(annotation);
}