class string {
    static native func len(s: String): Number
    static native func format(s: String, fmt: Array<Any?>): String
    static native func reverse(s: String): String
    static native func sub(s: String, from: Number, to: Number): String
    static native func repeat(s: String, n: Number, separator: String): String
}