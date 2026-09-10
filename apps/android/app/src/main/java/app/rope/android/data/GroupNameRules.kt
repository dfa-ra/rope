package app.rope.android.data

/**
 * Group titles are create-or-PATCH only. Fail closed on CR/LF/NUL before trim.
 * Length is Unicode runes (1–40), matching Go [utf8.RuneCountInString].
 */
object GroupNameRules {
    const val MIN = 1
    const val MAX = 40

    fun parse(raw: String?): String? {
        if (raw.isNullOrEmpty()) return null
        if (raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0 || raw.indexOf('\u0000') >= 0) {
            return null
        }
        val name = raw.trim()
        val runes = name.codePointCount(0, name.length)
        if (runes !in MIN..MAX) return null
        return name
    }
}
