class File {
    var filename: String = "";
    
    init(f: String){
        this.filename = f;
    }
    
    native func open(): File
    native func readBytes(): Array<Byte>
    native func readText(): String
}