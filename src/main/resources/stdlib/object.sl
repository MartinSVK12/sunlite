class Object {
    fun equals(o: Object): Boolean {
        return this == o;
    }
    
    fun getClass(): Class {
        return Object;
    }
    
    fun toString(): String {
        return str(this);
    }
}