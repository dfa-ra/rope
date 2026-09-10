package app.rope.android.data

/**
 * Share the one-time invite URL from the QR pane via ACTION_SEND.
 * Not clipboard copy. Reject CR/LF/NUL before trim. Scheme is rope://join only.
 */
object ShareInviteRules {
    const val LABEL = "Поделиться"
    const val CHOOSER = "Поделиться приглашением"
    const val PREFIX = "rope://join?"
    const val SEND_ACTION = "android.intent.action.SEND"
    const val EXTRA_TEXT = "android.intent.extra.TEXT"
    const val MIME = "text/plain"

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
