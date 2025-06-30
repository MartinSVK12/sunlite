class string {
    static native fun len(s: String): Number
    static native fun format(s: String, fmt: Array<Any?>): String
    static native fun reverse(s: String): String
    static native fun sub(s: String, from: Number, to: Number): String
    static native fun repeat(s: String, n: Number, separator: String): String
}