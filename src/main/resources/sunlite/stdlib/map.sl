import Iterator from "/sunlite/stdlib/iterable";
import Iterable from "/sunlite/stdlib/iterable";
import Map from "/sunlite/stdlib/map";
import HashMap from "/sunlite/stdlib/map";
import List from "/sunlite/stdlib/list";
import ArrayList from "/sunlite/stdlib/list";
import MapIterator from "/sunlite/stdlib/map";

//todo: unfinished

interface<K,V> Map implements Iterable {
    func size(): Int
    func get(key: Generic<K>): Generic<V>
    func getOrDefault(key: Generic<K>, default: Generic<V>): Generic<V>
    func remove(key: Generic<K>): Generic<V>
    func put(key: Generic<K>, value: Generic<V>): Generic<V>?
    func isEmpty(): Boolean
    func clear()
    func entries(): List<Tuple<Generic<K>, Generic<V>>>
}

class<K,V> MapIterator implements Iterator<Tuple<Generic<K>, Generic<V>>> {
    var source: List<Tuple<Generic<K>, Generic<V>>>? = nil;
    var _index: Int = 0;
    var _size: Int = 0;

    init(map: Map) {
        source = map.entries();
        _size = map.size();
    }

    override func next(): Tuple<Generic<K>, Generic<V>>? {
        val v := source.get(_index) as Tuple<Generic<K>, Generic<V>>?;
        _index++;
        return v;
    }
    override func current(): Tuple<Generic<K>, Generic<V>>? {
        return source.get(_index) as Tuple<Generic<K>, Generic<V>>?;
    }
    override func hasNext(): Boolean {
        return _index < _size;
    }
}

class HashMap implements Map {
    var _t = emptyTable(<Generic<K>, Generic<V>>);

    override func size(): Int {
        return sizeOf(_t);
    }

    override func get(key: Generic<K>): Generic<V> {
        return _t[key];
    }

    override func getOrDefault(key: Generic<K>, default: Generic<V>): Generic<V> {
        val v = get(key);
        if(v == nil) {
            return default;
        }
        return v;
    }

    override func put(key: Generic<K>, value: Generic<V>): Generic<V>? {
        val previous := get(key);
        _t[key] = value;
        return previous;
    }

    override func isEmpty(): Boolean {
        return size() == 0;
    }

    override func clear() {
        _t = emptyTable(<Generic<K>, Generic<V>>);
    }

    override func remove(key: Generic<K>): Generic<V> {
        val previous := get(key);
        _t[key] = nil;
        return previous;
    }

    override func entries(): List<Tuple<Generic<K>, Generic<V>>> {
        return tableEntries(<Generic<K>, Generic<V>> _t).toList(<Tuple<Generic<K>, Generic<V>>>);
    }

    override func getIterator(): Iterator<Tuple<Generic<K>, Generic<V>>> {
        return MapIterator(<Generic<K>, Generic<V>> this);
    }
}