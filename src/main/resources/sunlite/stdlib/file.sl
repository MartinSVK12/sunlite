class File {
    var filename: String = "";
    
    init(f: String){
        this.filename = f;
    }
    
    static native func openN(file: File): File
    static native func readBytesN(file: File): Array<Byte>
    static native func readTextN(file: File): String
    
    static func open(filename: String): File {
        val file: File = File(filename);
        return File.openN(file);
    }
    
    func readText(): String {
        return File.readTextN(this);
    }
    
    func readBytes(): Array<Byte> {
        return File.readBytesN(this);
    }
}