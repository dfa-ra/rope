package app.rope.android.data

/**
 * Copy the one-time invite URL from the QR pane.
 * Reject CR/LF/NUL before trim. Scheme is rope://join only.
 */
object InviteCopyRules {
    const val LABEL = "Скопировать ссылку"
    const val PREFIX = "rope://join?"

    fun accept(raw: String): String? {
        if (raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0 || raw.indexOf('\u0000') >= 0) {
            return null
        }
        val url = raw.trim()
        if (!url.startsWith(PREFIX)) return null
        if (url.length <= PREFIX.length) return null
        return url
    }

    fun enabled(raw: String): Boolean = accept(raw) != null
}
