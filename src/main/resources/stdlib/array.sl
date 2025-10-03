class array {
    static fun forEach(arr: Array, block: Function<Any?, Nil>) {
        for (var i: Number = 0; i < sizeOf(arr); i = i + 1){
            block(arr[i]);
        }
    }
}