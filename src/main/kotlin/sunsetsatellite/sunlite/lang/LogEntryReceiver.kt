package sunsetsatellite.sunlite.lang

interface LogEntryReceiver {

    fun info(message: String)
    fun warn(message: String)
    fun err(message: String)

}