package app.rope.android.data

/**
 * PATCH /v1/me display names. Fail closed on CR/LF/NUL before trim.
 * Same charset as bootstrap login: 2–24 letters/digits/_ . -
 */
object MeNameRules {
    const val MIN = 2
    const val MAX = 24

    fun parse(raw: String?): String? {
        if (raw.isNullOrEmpty()) return null
        if (raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0 || raw.indexOf('\u0000') >= 0) {
            return null
        }
        val name = raw.trim()
        val runes = name.codePointCount(0, name.length)
        if (runes !in MIN..MAX) return null
        if (!name.all { it.isLetterOrDigit() || it == '_' || it == '-' || it == '.' }) return null
        return name
    }
}
