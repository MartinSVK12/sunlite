package sunsetsatellite.sunlite.lang

interface LogEntryReceiver {

    fun debug(message: String)
    fun info(message: String)
    fun warn(message: String)
    fun err(message: String)

}