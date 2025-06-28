fun inc(arr: array<any|nil>) {
    resize(arr,sizeOf(arr)+1);
}

fun dec(arr: array<any|nil>) {
    resize(arr,sizeOf(arr)-1);
}

class<T> List {
    
    var _l: number = 0;
    var _a: array<generic<T>> = arrayOf(10);
    
    init(){
        
    }
    
    fun size(): number { return this._l; }
    
    fun isEmpty(): boolean {
        return this.size() == 0;
    }
    
    fun insert(index: number, o: generic<T>): boolean {
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
    
    fun add(o: generic<T>): boolean {
        if(this._l > sizeOf(this._a)){
            inc(this._a);
        }
        this._a[this._l] = o;
        this._l = this._l + 1;
        return true;
    }
    
    fun remove(o: generic<T>): boolean {
        if(!this.contains(o)) return false;
        var index: number = this.indexOf(o);
        return this.removeAt(index);
    }
    
    fun removeAt(index: number): boolean {
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
    
    fun get(i: number): generic<T> {
        return this._a[i] as generic<T>;
    }
    
    fun indexOf(o: generic<T>): number {
        for(var i: number = 0; i < this._l; i = i + 1){
            if(this.get(i) == o){
                return i;
            }
        }
        return -1;
    }

    fun contains(o: generic<T>): boolean {
        for(var i: number = 0; i < this._l; i = i + 1){
            if(this.get(i) == o){
                return true;
            }
        }
        return false;
    }

    
    fun forEach(callback: function<any|nil, nil>) {
        for(var i: number = 0; i < this._l; i = i + 1){
            callback(this.get(i));
        }
    }

    fun forEachIndexed(callback: function<number, any|nil, nil>) {
        for(var i: number = 0; i < this._l; i = i + 1){
            callback(i,this.get(i));
        }
    }
}

var list: List<number> = List(<number>);
list.add(5);
//list.removeAt(1);
print(list);

list.forEach( fun(o: number) { print(o); });