import Iterator from "/sunlite/stdlib/iterable.sl";
import Iterable from "/sunlite/stdlib/iterable.sl";

interface<T> List implements Iterable {
    func size(): Int
    func isEmpty(): Boolean
    func insert(index: Int, o: Generic<T>): Boolean
    func add(o: Generic<T>): Boolean
    func addAll(list: List<Generic<T>>)
    func remove(o: Generic<T>): Boolean
    func removeAt(index: Int): Boolean
    func clear()
    func get(i: Int): Generic<T>
    func indexOf(o: Generic<T>): Int
    func contains(o: Generic<T>): Boolean
    func forEach(callback: Function<Generic<T>, Nil>)
    func forEachIndexed(callback: Function<Int, Generic<T>, Nil>)
    func filter(callback: Function<Generic<T>, Boolean>): List<Generic<T>>
    func<U> map(callback: Function<Generic<T>, Generic<U>>): List<Generic<U>>
    func any(callback: Function<Generic<T>, Boolean>): Boolean
    func all(callback: Function<Generic<T>, Boolean>): Boolean
    func none(callback: Function<Generic<T>, Boolean>): Boolean
}

class ListIterator implements Iterator {
    var source: List? = nil;
    var index: Int = 0;
    var size: Int = 0;

    init(list: List) {
        source = list;
        size = list.size();
    }
    
    override func next(): Any? {
        val v = source.get(index);
        index++;
        return v;
    }
    override func current(): Any? {
        return source.get(index);
    }
    override func hasNext(): Boolean {
        return index < size;
    }
}

class<T> ArrayList implements List {

    var _l: Int = 0;
    var _a: Array<Generic<T>> = arrayOf(10);

    override func size(): Int { return _l; }

    override func isEmpty(): Boolean {
        return size() == 0;
    }

    override func insert(index: Int, o: Generic<T>): Boolean {
        if(_l <= index) return false;
        if(_l > sizeOf(_a)){
            inc(_a);
        }
        for(var i: Int = _l; i > index; i = i - 1){
            _a[i] = _a[i - 1];
        }
        _a[index] = o;
        _l = _l + 1;
        return true;
    }

    override func add(o: Generic<T>): Boolean {
        if(_l >= sizeOf(_a)){
            inc(_a);
        }
        _a[_l] = o;
        _l = _l + 1;
        return true;
    }

    override func addAll(list: List<Generic<T>>) {
        list.forEach(func(o: Generic<T>){this.add(o);});
    }

    override func remove(o: Generic<T>): Boolean {
        if(!contains(o)) return false;
        var index: Int = indexOf(o);
        return removeAt(index);
    }

    override func removeAt(index: Int): Boolean {
        if(index != -1){
            //_a[index] = nil;
            for(var i: Int = index; i < _l; i = i + 1){
                _a[i] = _a[i + 1];
            }
            _l = _l - 1;
            dec(_a);
            return true;
        }
        return false;
    }

    override func clear() {
        _l = 0;
        resize(arr,1);
    }

    override func get(i: Int): Generic<T> {
        return _a[i];
    }

    override func indexOf(o: Generic<T>): Int {
        for(var i: Int = 0; i < _l; i = i + 1){
            if(get(i) == o){
                return i;
            }
        }
        return -1;
    }

    override func contains(o: Generic<T>): Boolean {
        for(var i: Int = 0; i < _l; i = i + 1){
            if(get(i) == o){
                return true;
            }
        }
        return false;
    }


    override func forEach(callback: Function<Generic<T>, Nil>) {
        for(var i: Int = 0; i < _l; i = i + 1){
            callback(get(i));
        }
    }

    override func forEachIndexed(callback: Function<Int, Generic<T>, Nil>) {
        for(var i: Int = 0; i < _l; i = i + 1){
            callback(i,get(i));
        }
    }
    
    override func any(callback: Function<Generic<T>, Boolean>): Boolean {
        foreach(var o: Generic<T> in this){
            if(callback(o)) {
                return true;
            }
        }
        return false;
    }
    override func all(callback: Function<Generic<T>, Boolean>): Boolean {
        foreach(var o: Generic<T> in this){
            if(!callback(o)) {
                return false;
            }
        }
        return true;
    }
    override func none(callback: Function<Generic<T>, Boolean>): Boolean {
        foreach(var o: Generic<T> in this){
            if(callback(o)) {
                return false;
            }
        }
        return true;
    }

    override func filter(callback: Function<Generic<T>,Boolean>): List<Generic<T>> {
        val list: List<Generic<T>> = ArrayList(<Generic<T>>);
        this.forEach(func(o: Generic<T>){ if(callback(o)) { list.add(o as Generic<T>); }});
        return list;
    }

    override func<U> map(callback: Function<Generic<T>, Generic<U>>): List<Generic<U>> {
        val list: List<Generic<U>> = ArrayList(<Generic<U>>);
        this.forEach(func(o: Generic<T>){ list.add(callback(o)); });
        return list;
    }

    func inc(arr: Array<Any|Nil>) {
        resize(arr,sizeOf(arr)+1);
    }

    func dec(arr: Array<Any|Nil>) {
        resize(arr,sizeOf(arr)-1);
    }

    override func getIterator(): Iterator {
        return ListIterator(this);
    }
}