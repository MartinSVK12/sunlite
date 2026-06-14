import ArrayIterator from "/sunlite/stdlib/array.sl";

class IntRange implements Iterable {
    
    var begin: Int;
    var end: Int;
    var size: Int;
    
    init(begin: Int, end: Int) {
        this.begin = begin;
        this.end = end;
        size = end - begin;
    }
    
    override func getIterator(): Iterator {
        val arr: Array<Int> = emptyArray(<Int> size);
        for(var i: Int = this.begin; i < this.end; i++){
            arr[i] = i;
        }
        return ArrayIterator(arr);
    }
}