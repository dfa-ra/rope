package app.rope.android.data

/**
 * Long-press the reply quote chrome to copy the quoted text.
 * Reject NUL before trim. Newlines stay — this is message body, not a URL.
 */
object QuoteCopyRules {
    const val LABEL = "Копировать цитату"

    fun clipboard(preview: String): String? {
        if (preview.indexOf('\u0000') >= 0) return null
        val t = preview.trim()
        return t.ifEmpty { null }
    }

    fun enabled(preview: String): Boolean = clipboard(preview) != null
}
