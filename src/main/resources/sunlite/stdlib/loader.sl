import ModuleLoader from "/sunlite/stdlib/loader";

interface ModuleLoader {
    func load(name: String, path: String): Function<Nil>
    func parent(): ModuleLoader?
}

class BaseModuleLoader implements ModuleLoader {
    override func load(name: String, path: String): Function<Nil> {
        return loadNative(name, path);
    }

    native func loadNative(name: String, path: String): Function<Nil>

    override func parent(): ModuleLoader? {
        return nil;
    }
}