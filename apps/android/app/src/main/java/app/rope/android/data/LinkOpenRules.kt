package app.rope.android.data

import java.net.URI

/**
 * Confirm before opening an https:// link from a message.
 * Reject CR/LF/NUL before trim. Not FCM.
 */
object LinkOpenRules {
    const val TITLE = "Открыть ссылку"
    const val CONFIRM = "Открыть"
    const val CANCEL = "Отмена"

    fun accept(raw: String): String? {
        if (raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0 || raw.indexOf('\u0000') >= 0) {
            return null
        }
        val url = raw.trim()
        if (!url.startsWith("https://", ignoreCase = true)) return null
        if (url.length < 10) return null
        return url
    }

    fun host(url: String): String {
        val accepted = accept(url) ?: return ""
        val host = runCatching { URI(accepted).host }.getOrNull()
            ?.trim()
            ?.trim('.')
            ?.lowercase()
            .orEmpty()
        return host.ifBlank { accepted }
    }

    fun body(url: String): String = host(url)
}
