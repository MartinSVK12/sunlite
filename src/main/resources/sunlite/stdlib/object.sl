class Object {    
    func toString(): String {
        return str(this);
    }

    func equals(other: Any?){
        return this == other;
    }

    func hashCode(): Int {
        return hash(this);
    }
}