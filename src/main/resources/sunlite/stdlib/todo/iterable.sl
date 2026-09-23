interface<T> Iterator {
    func next(): Generic<T>?
    func current(): Generic<T>?
    func hasNext(): Boolean
}

interface<T> Iterable {
    func getIterator(): Iterator<Generic<T>>
}