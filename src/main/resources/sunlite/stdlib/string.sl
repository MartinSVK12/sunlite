class string {
    static native func len(s: String): Int
    static native func format(s: String, fmt: Array<Any?>): String
    static native func reverse(s: String): String
    static native func sub(s: String, begin: Int, end: Int): String
    static native func at(s: String, index: Int): String
    static native func repeat(s: String, n: Int, separator: String): String
    static native func contains(s: String, other: String): Boolean
    static native func replace(s: String, replace: String, with: String): String
    static native func trim(s: String): String
}