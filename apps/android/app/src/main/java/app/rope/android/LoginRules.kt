package app.rope.android

object LoginRules {
    fun normalize(raw: String): String = raw.trim()

    fun isValid(raw: String): Boolean {
        val s = normalize(raw)
        if (s.length !in 2..24) return false
        return s.all { it.isLetterOrDigit() || it == '_' || it == '-' || it == '.' }
    }
}
