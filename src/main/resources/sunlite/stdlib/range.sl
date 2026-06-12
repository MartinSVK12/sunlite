class IntRange {
    var _begin: Int = 0;
    var _end: Int = 0;
    
    init(begin: Int, end: Int) {
        _begin = begin;
        _end = end;
    }
    
    func range(): Array<Int> {
        val arr: Array<Int> = emptyArray(_end);
        for(var i: Int = _begin; i < _end; i = i + 1){
            arr[i] = i;
        }
        return arr;
    }
}