import "/iterable.sl";

class array {
    static func forEach(arr: Array, block: Function<Any?, Nil>) {
        for (var i: Int = 0; i < sizeOf(arr); i = i + 1){
            block(arr[i]);
        }
    }
    
    static func getIterator(arr: Array): ArrayIterator {
        return ArrayIterator(arr);
    }
}
