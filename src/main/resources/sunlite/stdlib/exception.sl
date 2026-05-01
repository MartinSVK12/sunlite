class Exception {
    var message: String = "";
    var stacktrace: Array<String> = getStacktrace(true);
    var cause: Exception? = nil;
    
    init(msg: String){
        message = msg;
    }
    
    init(msg: String, causedBy: Exception){
        message = msg;
        cause = causedBy;
    }

}