interface Iterator {
    func next(): Any?
    func current(): Any?
    func hasNext(): Boolean
}

interface Iterable {
    func getIterator(): Iterator
}