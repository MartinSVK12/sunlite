import Iterator from "/sunlite/stdlib/iterable";
import Iterable from "/sunlite/stdlib/iterable";
import List from "/sunlite/stdlib/list";
import ArrayList from "/sunlite/stdlib/list";

class Arrays {
    static func forEach(arr: Array, block: Function<Any?, Nil>) {
        for (var i: Int = 0; i < sizeOf(arr); i = i + 1){
            block(arr[i]);
        }
    }
    
    static func getIterator(arr: Array): ArrayIterator {
        return ArrayIterator(arr);
    }

    static func printContents(arr: Array) {
        for (var i: Int = 0; i < sizeOf(arr); i = i + 1){
            print(arr[i]);
        }
    }

    static func<T> toList(arr: Array): List<Generic<T>> {
        return ArrayList(<Generic<T>>arr);
    }
}

class ArrayIterator implements Iterator<Any?> {
    
    var _index: Int = 0;
    var _array: Array<Any?>? = nil;
    
    init(arr: Array<Any?>?) {
        _array = arr;
    }

    override func current(): Any? {
        return _array[_index] as Any?;
    }
    
    override func next(): Any? {
        val v = _array[_index] as Any?;
        _index = _index + 1;
        return v;
    }
    
    override func hasNext(): Boolean {
        if(_index < sizeOf(_array)){
            return true;
        }
        return false;
    }
}