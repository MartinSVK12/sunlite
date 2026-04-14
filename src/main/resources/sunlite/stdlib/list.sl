func inc(arr: Array<Any|Nil>) {
    resize(arr,sizeOf(arr)+1);
}

func dec(arr: Array<Any|Nil>) {
    resize(arr,sizeOf(arr)-1);
}

class List {
    
    var _l: Int = 0;
    var _a: Array<Any?> = arrayOf(10);
    
    func size(): Int { return this._l; }
    
    func isEmpty(): Boolean {
        return this.size() == 0;
    }
    
    func insert(index: Int, o: Any?): Boolean {
        if(this._l <= index) return false;
        if(this._l > sizeOf(this._a)){
            inc(this._a);
        }
        for(var i: Int = this._l; i > index; i = i - 1){
            this._a[i] = this._a[i - 1];
        }
        this._a[index] = o;
        this._l = this._l + 1;
        return true;
    }
    
    func add(o: Any?): Boolean {
        if(this._l >= sizeOf(this._a)){
            inc(this._a);
        }
        this._a[this._l] = o;
        this._l = this._l + 1;
        return true;
    }
    
    func remove(o: Any?): Boolean {
        if(!this.contains(o)) return false;
        var index: Int = this.indexOf(o);
        return this.removeAt(index);
    }
    
    func removeAt(index: Int): Boolean {
        if(index != -1){
            this._a[index] = nil;
            for(var i: Int = index; i < this._l; i = i + 1){
                this._a[i] = this._a[i + 1];
            }
            this._l = this._l - 1;
            dec(this._a);
            return true;
        }
        return false;
    }
    
    func get(i: Int): Any? {
        return this._a[i] as Any?;
    }
    
    func indexOf(o: Any?): Int {
        for(var i: Int = 0; i < this._l; i = i + 1){
            if(this.get(i) == o){
                return i;
            }
        }
        return -1;
    }

    func contains(o: Any?): Boolean {
        for(var i: Int = 0; i < this._l; i = i + 1){
            if(this.get(i) == o){
                return true;
            }
        }
        return false;
    }

    
    func forEach(callback: Function<Any|Nil, Nil>) {
        for(var i: Int = 0; i < this._l; i = i + 1){
            callback(this.get(i));
        }
    }

    func forEachIndexed(callback: Function<Int, Any|Nil, Nil>) {
        for(var i: Int = 0; i < this._l; i = i + 1){
            callback(i,this.get(i));
        }
    }
}