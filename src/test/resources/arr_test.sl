fun<T> forEach(arr: Array<Generic<T>>,block: Function<Generic<T>, Nil>) {
    for (var i = 0; i < sizeOf(arr); i = i + 1){
        block(arr[i]);
    }
}

val arr: Array<Number> = arrayOf(1,2,3,4,5,6,7,8,9,0) as Array<Number>;

forEach(arr, fun(n: Number){
    print(n);
} as Function<Generic<T>, Nil>);