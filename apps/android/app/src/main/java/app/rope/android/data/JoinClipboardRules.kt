package app.rope.android.data

/**
 * Join **Вставить** fills a `rope://join` URL from the clipboard.
 * Distinct from composer paste-image and provision SSH paste.
 * LocalStore schema stays v6.
 */
object JoinClipboardRules {
    const val LABEL = "Вставить"
    const val PREFIX = "rope://join"
    const val EMPTY = "В буфере нет ссылки"

    fun accept(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        if ('\n' in raw || '\r' in raw || '\u0000' in raw) return null
        val s = raw.trim()
        if (!s.startsWith(PREFIX)) return null
        return s
    }
}
