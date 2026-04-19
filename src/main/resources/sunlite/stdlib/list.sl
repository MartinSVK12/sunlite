class<T> List {
    
    var _l: Int = 0;
    var _a: Array<Generic<T>> = arrayOf(10);
    
    func size(): Int { return _l; }
    
    func isEmpty(): Boolean {
        return size() == 0;
    }
    
    func insert(index: Int, o: Generic<T>): Boolean {
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
    
    func add(o: Generic<T>): Boolean {
        if(_l >= sizeOf(_a)){
            inc(_a);
        }
        _a[_l] = o;
        _l = _l + 1;
        return true;
    }
    
    func addAll(list: List<Generic<T>>) {
        list.forEach(func(o: Generic<T>){this.add(o);});
    }
    
    func remove(o: Generic<T>): Boolean {
        if(!contains(o)) return false;
        var index: Int = indexOf(o);
        return removeAt(index);
    }
    
    func removeAt(index: Int): Boolean {
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
    
    func get(i: Int): Generic<T> {
        return _a[i];
    }
    
    func indexOf(o: Generic<T>): Int {
        for(var i: Int = 0; i < _l; i = i + 1){
            if(get(i) == o){
                return i;
            }
        }
        return -1;
    }

    func contains(o: Generic<T>): Boolean {
        for(var i: Int = 0; i < _l; i = i + 1){
            if(get(i) == o){
                return true;
            }
        }
        return false;
    }

    
    func forEach(callback: Function<Generic<T>, Nil>) {
        for(var i: Int = 0; i < _l; i = i + 1){
            callback(get(i));
        }
    }

    func forEachIndexed(callback: Function<Int, Generic<T>, Nil>) {
        for(var i: Int = 0; i < _l; i = i + 1){
            callback(i,get(i));
        }
    }

    //func filter(callback: Function<Generic<T>,Boolean>): List<Generic<T>> {
    //    val list: List<Generic<T>> = List(<Generic<T>>);
    //    this.forEach(func(o: Generic<T>){ if(callback(o)) { list.add(o as Generic<T>); }});
    //    return list;
    //}

    func inc(arr: Array<Any|Nil>) {
        resize(arr,sizeOf(arr)+1);
    }

    func dec(arr: Array<Any|Nil>) {
        resize(arr,sizeOf(arr)-1);
    }
}