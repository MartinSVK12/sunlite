abstract class Enum {
    static native func entries(e: Enum | Class<Enum>): Array<Enum>
    static native func fromName(e: Enum | Class<Enum>, s: String): Enum
}