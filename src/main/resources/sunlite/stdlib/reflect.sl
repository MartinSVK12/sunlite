class reflect {    
    static native func getMethods(c: Class): Array<String>
    static native func getFields(c: Class): Array<String>
}