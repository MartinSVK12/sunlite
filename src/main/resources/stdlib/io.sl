class IO {    
    static native func openNative(file: File): File
    static func open(filename: String): File {
        val file: File = File(filename);
        return IO.openNative(file);
    }
}

class File {
    var filename: String = "";
    
    init(f: String){
        this.filename = f;
    }
}