interface Iterator {
    func next(): Any?
    func current(): Any?
    func hasNext(): Boolean
}

interface Iterable {
    func getIterator(): Iterator
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