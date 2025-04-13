package sunsetsatellite.lang.sunlite

interface LogEntryReceiver {

    fun info(message: String)
    fun warn(message: String)
    fun err(message: String)

}