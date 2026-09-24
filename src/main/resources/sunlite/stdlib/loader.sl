module std::loader;

interface ModuleLoader {
    func findClass(name: String): Class?
    func loadModule(name: String, path: String): Function<Nil>?
}