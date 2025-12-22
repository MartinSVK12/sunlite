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

class ArrayIterator implements Iterator {
    
    var _index: Int = 0;
    var _array: Array<Any?>? = nil;
    
    init(arr: Array<Any?>?) {
        this._array = arr;
    }

    func current(): Any? {
        return this._array[this._index] as Any?;
    }
    
    func next(): Any? {
        val v = this._array[this._index] as Any?;
        this._index = this._index + 1;
        return v;
    }
    
    func hasNext(): Boolean {
        if(this._index < sizeOf(this._array)){
            return true;
        }
        return false;
    }
}