interface Iterator {
    fun next(): Any?
    fun current(): Any?
    fun hasNext(): Boolean
}

interface Iterable {
    fun getIterator(): Iterator
}

class ArrayIterator implements Iterator {
    
    var _index: Number = 0;
    var _array: Array<Any?>? = nil;
    
    init(arr: Array<Any?>?) {
        this._array = arr;
    }

    fun current(): Any? {
        return this._array[this._index] as Any?;
    }
    
    fun next(): Any? {
        val v = this._array[this._index] as Any?;
        this._index = this._index + 1;
        return v;
    }
    
    fun hasNext(): Boolean {
        if(this._index < sizeOf(this._array)){
            return true;
        }
        return false;
    }
}