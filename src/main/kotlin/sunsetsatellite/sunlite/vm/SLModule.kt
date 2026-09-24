package sunsetsatellite.sunlite.vm

import sunsetsatellite.sunlite.lang.Sunlite
import sunsetsatellite.sunlite.lang.Sunlite.Companion.debug
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.notExists

class SLModule(
    val name: String,
    val path: Path,
    var empty: Boolean,
    val contents: MutableList<Path> = mutableListOf(),
    val env: MutableMap<String, AnySLValue> = mutableMapOf(),
    var initializer: SLClosureObj? = null,
    var loader: SLClassInstance? = null
) {
    var initialized = false

    fun findModule(vm: VM, name: String): AnySLValue? {
        if(debug){
            vm.sunlite.printDebug("Finding module: '$name' in '${this.name}'.")
        }
        loadModule(vm)
        runModule(vm)
        if(env.containsKey(name)){
            return env[name]
        }
        contents.find { it.fileName.nameWithoutExtension == name }?.let {
            val module = getModule(vm, it) ?: return null
            env[name] = module
            return module
        }
        return null
    }

    fun runModule(vm: VM) {
        if(!initialized && !empty && initializer != null){
            initialized = true
            vm.sunlite.printDebug("Activating module: '${this.name}'.")
            vm.internalCall(initializer!!)
        } else if(!initialized) {
            initialized = true
        }
    }

    fun getModule(vm: VM, path: Path): SLModuleObj? {
        if(loader != null){
            //todo:
            return null
        } else {
            val fullPath = this.path.resolve(path)
            if(fullPath.notExists()) return null
            val module = getModuleNative(vm, path) ?: return null
            return SLModuleObj(module)
        }
    }

    fun loadModule(vm: VM) {
        vm.sunlite.printDebug("Loading module: '${this.name}'.")
        if(initializer == null && !empty){
            if(loader != null){
                //todo:
                return
            } else {
                loadModuleNative(vm)
            }
        }
    }

    fun loadModuleNative(vm: VM){
        if(path.notExists()){
            vm.runtimeError("Could not load module '$name'!")
            return
        }
        val stream: InputStream = vm.sunlite.readStreamFunction.apply(path.toString())
        DataInputStream(stream.buffered()).use { s ->
            try {
                val program: SLFunction = SLFunction.read(s)
                //vm.moduleCache[name] = program
                program.chunk.debugInfo.classData.forEach { (string, data) ->
                    vm.classes[string] = data
                }
                initializer = SLClosureObj(SLClosure(program))
            } catch (e: Exception){
                vm.runtimeError("Could not load module '$name': ${e.message}")
                return
            }
        }
        if(initializer == null){
            vm.runtimeError("Could not load module '$name'!")
            return
        }
        //vm.call(initializer!!,0)
        //vm.currentFrame = vm.frameStack.peek()
    }

    fun getModuleNative(vm: VM, name: Path): SLModule? {
        val modulePath = this.path.resolve(name)
        var url = Sunlite::class.java.getResource(modulePath.toString().replace("\\","/"))
        if(url == null){
            try {
                url = modulePath.toUri().toURL()
            } catch (e: Exception){ }
        }
        var path: Path? = null
        val invalidPaths: MutableList<String> = mutableListOf()
        if(url == null){
            vm.sunlite.path.forEach { p ->
                try {
                    Path(p, name.toString()).let {
                        if(it.exists()){
                            path = it
                            return@forEach
                        }
                    }
                    return@forEach
                } catch (_: IOException) {
                    invalidPaths.add(p)
                }
            }
        } else {
            url.path.substring(1).let { p ->
                if(p.isNotEmpty()){
                    Path(p).let {
                        if(it.exists()){
                            path = it
                        }
                    }
                }
            }
        }
        if(path == null){
            vm.runtimeError("Could not find module '$name'!")
            return null
        }
        val contents = if(path.isDirectory()) path.listDirectoryEntries().map { path.relativize(it) } else listOf<Path>()
        return SLModule(name.toString(), path, path.isDirectory(), contents.toMutableList())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SLModule) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return "<module '$name'>"
    }
}