//class Module {
//    var name: String = "";
//    var path: String = "";
//    var contents: Array<String>? = nil;
//}

interface ModuleLoader {
    //func get(name: String, path: String): Module?
    //func load(name: String, path: String): Function<Nil>
    func parent(): ModuleLoader?
}

//class BaseModuleLoader implements ModuleLoader {
//    override func get(name: String, path: String): Module? {
//        return getNative(name, path, Module());
//    }
//
//    override func load(name: String, path: String): Function<Nil> {
//        return loadNative(name, path);
//    }
//
//    native func getNative(name: String, path: String, m: Module): Module?
//
//    native func loadNative(name: String, path: String): Function<Nil>
//
//    override func parent(): ModuleLoader? {
//        return nil;
//    }
//}