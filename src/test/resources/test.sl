class<T> A {

    var test: Generic<T> = "";

    init() {

    }
}

val a: A<String> = A(<String>);

a.test = 2;