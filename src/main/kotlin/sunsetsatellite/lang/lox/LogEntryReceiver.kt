package sunsetsatellite.lang.lox

interface LogEntryReceiver {

    fun info(message: String)
    fun warn(message: String)
    fun err(message: String)

}