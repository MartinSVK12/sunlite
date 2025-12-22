class IntRange {
    var _begin: Int = 0;
    var _end: Int = 0;
    
    init(begin: Int, end: Int) {
        this._begin = begin;
        this._end = end;
    }
    
    func range(): Array<Int> {
        val arr: Array<Int> = emptyArray(this._end);
        for(var i: Int = this._begin; i < this._end; i = i + 1){
            arr[i] = i;
        }
        return arr;
    }
}