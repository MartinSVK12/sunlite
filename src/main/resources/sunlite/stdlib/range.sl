import ArrayIterator from "/sunlite/stdlib/array.sl";

class IntRange implements Iterable<Int> {
    
    var begin: Int;
    var end: Int;
    var size: Int;
    
    init(begin: Int, end: Int) {
        this.begin = begin;
        this.end = end;
        size = end - begin;
    }
    
    override func getIterator(): Iterator<Int> {
        val arr: Array<Int> = emptyArray(<Int> size);
        var j: Int = 0;
        for(var i: Int = this.begin; i < this.end; i++){
            arr[j] = i;
            j++;
        }
        return ArrayIterator(arr);
    }
}

class CharRange implements Iterable<String> {

    var begin: Int;
    var end: Int;
    var size: Int;

    init(begin: String, end: String) {
        this.begin = ord(begin);
        this.end = ord(end);
        size = (this.end - this.begin) + 1;
    }

    override func getIterator(): Iterator<String> {
        val arr: Array<String> = emptyArray(<String> size);
        var j: Int = 0;
        for(var i: Int = this.begin; i <= this.end; i++){
            arr[j] = chr(i);
            j++;
        }
        return ArrayIterator(arr);
    }
}