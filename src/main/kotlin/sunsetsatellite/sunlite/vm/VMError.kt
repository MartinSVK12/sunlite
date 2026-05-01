package sunsetsatellite.sunlite.vm

class VMError(override val message: String, override val cause: Throwable? = null) : Exception(message, cause) {

}