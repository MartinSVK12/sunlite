abstract class Enum {
    static native func entries(): Array<Enum>
    static native func fromName(s: String): Enum
}