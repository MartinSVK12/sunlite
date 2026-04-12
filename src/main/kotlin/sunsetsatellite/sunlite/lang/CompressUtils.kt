package sunsetsatellite.sunlite.lang

import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.commons.io.IOUtils
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.nio.file.Files
import java.nio.file.Path


object CompressUtils {
    fun compress(file: Path, destination: Path) {
        try {
            Files.newOutputStream(destination).use {
                out ->
                        BufferedOutputStream(out).use {
                    buffer ->
                            CompressorStreamFactory()
                                    .createCompressorOutputStream("gz", buffer).use {
                        compressor ->
                                IOUtils.copy(Files.newInputStream(file), compressor)
                    }
                }
            }
        } catch (e:Exception){
            throw RuntimeException(e)
        }
    }
}