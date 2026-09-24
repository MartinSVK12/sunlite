package sunsetsatellite.sunlite.vm

enum class VMExceptions(val className: String = "sunlite::stdlib::exception::Exception") {
    TYPE_ERROR,
    INDEX_OUT_OF_BOUNDS,
    STACK_OVERFLOW,
    INVALID_OPERATION,
    INVALID_STATE,
    INVALID_ARGUMENTS,
    UNDEFINED_VALUE,
    NO_METHOD,
    NO_FIELD,
    NO_CLASS,
    NO_MODULE,
    IO_ERROR,
    UNKNOWN
}