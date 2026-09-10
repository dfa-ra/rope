package app.rope.android.data

/**
 * Local «-yourself line on Settings. Not a nickname, not display name, not server.
 */
object BioRules {
    const val KEY = "about"
    const val TITLE = "О себе"
    const val MAX = 70

    fun sanitize(raw: String?): String {
        if (raw.isNullOrEmpty()) return ""
        if ('\n' in raw || '\r' in raw || '\u0000' in raw) {
            return raw.filter { it != '\n' && it != '\r' && it != '\u0000' }.take(MAX)
        }
        return if (raw.length <= MAX) raw else raw.take(MAX)
    }

    fun remaining(raw: String?): Int = (MAX - sanitize(raw).length).coerceAtLeast(0)

    fun hint(): String =
        "Только на этом телефоне. Не имя в справочнике и не ник контакта."
}
