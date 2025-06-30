fun inc(arr: Array<Any|Nil>) {
    resize(arr,sizeOf(arr)+1);
}

fun dec(arr: Array<Any|Nil>) {
    resize(arr,sizeOf(arr)-1);
}

class<T> List {
    
    var _l: Number = 0;
    var _a: Array<Generic<T>> = arrayOf(10);
    
    init(){
        print("hi");
    }
    
    fun size(): Number { return this._l; }
    
    fun isEmpty(): Boolean {
        return this.size() == 0;
    }
    
    fun insert(index: Number, o: Generic<T>): Boolean {
        if(this._l <= index) return false;
        if(this._l > sizeOf(this._a)){
            inc(this._a);
        }
        for(var i = this._l; i > index; i = i - 1){
            this._a[i] = this._a[i - 1];
        }
        this._a[index] = o;
        this._l = this._l + 1;
        return true;
    }
    
    fun add(o: Generic<T>): Boolean {
        if(this._l > sizeOf(this._a)){
            inc(this._a);
        }
        this._a[this._l] = o;
        this._l = this._l + 1;
        return true;
    }
    
    fun remove(o: Generic<T>): Boolean {
        if(!this.contains(o)) return false;
        var index: Number = this.indexOf(o);
        return this.removeAt(index);
    }
    
    fun removeAt(index: Number): Boolean {
        if(index != -1){
            this._a[index] = nil;
            for(var i = index; i < this._l; i = i + 1){
                this._a[i] = this._a[i + 1];
            }
            this._l = this._l - 1;
            dec(this._a);
            return true;
        }
        return false;
    }
    
    fun get(i: Number): Generic<T> {
        return this._a[i] as Generic<T>;
    }
    
    fun indexOf(o: Generic<T>): Number {
        for(var i: Number = 0; i < this._l; i = i + 1){
            if(this.get(i) == o){
                return i;
            }
        }
        return -1;
    }

    fun contains(o: Generic<T>): Boolean {
        for(var i: Number = 0; i < this._l; i = i + 1){
            if(this.get(i) == o){
                return true;
            }
        }
        return false;
    }

    
    fun forEach(callback: Function<Any|Nil, Nil>) {
        for(var i: Number = 0; i < this._l; i = i + 1){
            callback(this.get(i));
        }
    }

    fun forEachIndexed(callback: Function<Number, Any|Nil, Nil>) {
        for(var i: Number = 0; i < this._l; i = i + 1){
            callback(i,this.get(i));
        }
    }
}