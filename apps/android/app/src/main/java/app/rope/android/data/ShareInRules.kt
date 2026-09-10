package app.rope.android.data

/**
 * Inbound ACTION_SEND / ACTION_SEND_MULTIPLE from another app. The user
 * picks a Rope chat; URIs stage like Attach and extra text becomes the draft.
 * Reject CR/LF/NUL. LocalStore stays v6. Not FCM.
 */
object ShareInRules {
    const val TITLE = "Отправить в Rope"
    const val PICK = "Выберите чат"
    const val ACTION_SEND = "android.intent.action.SEND"
    const val ACTION_SEND_MULTIPLE = "android.intent.action.SEND_MULTIPLE"
    const val EXTRA_CONSUMED = "app.rope.android.extra.SHARE_CONSUMED"
    const val MAX_URI = 2048
    const val MAX_TEXT = 4096

    data class Inbound(
        val uris: List<String> = emptyList(),
        val text: String? = null,
    ) {
        val active: Boolean get() = uris.isNotEmpty() || !text.isNullOrBlank()
    }

    fun isSend(action: String?): Boolean =
        action == ACTION_SEND || action == ACTION_SEND_MULTIPLE

    fun alreadyConsumed(consumed: Boolean): Boolean = consumed

    fun active(uris: List<String>, text: String?): Boolean =
        uris.isNotEmpty() || !text.isNullOrBlank()

    fun sanitizeUri(raw: String?): String? {
        if (raw == null) return null
        if (raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0 || raw.indexOf('\u0000') >= 0) {
            return null
        }
        val u = raw.trim()
        if (u.isEmpty() || u.length > MAX_URI) return null
        val lower = u.lowercase()
        if (lower.startsWith("content://") || lower.startsWith("file://")) return u
        return null
    }

    fun sanitizeText(raw: String?): String? {
        if (raw == null) return null
        if (raw.indexOf('\u0000') >= 0) return null
        val t = raw.trim()
        if (t.isEmpty()) return null
        return if (t.length <= MAX_TEXT) t else t.take(MAX_TEXT)
    }

    fun consume(
        action: String?,
        stream: String?,
        streams: List<String>,
        text: String?,
        consumed: Boolean = false,
    ): Inbound? {
        if (alreadyConsumed(consumed) || !isSend(action)) return null
        val uris = buildList {
            sanitizeUri(stream)?.let { add(it) }
            for (s in streams) {
                val u = sanitizeUri(s) ?: continue
                if (u !in this) add(u)
                if (size >= AlbumRules.MAX_PHOTOS) break
            }
        }
        val body = sanitizeText(text)
        val inbound = Inbound(uris, body)
        return inbound.takeIf { it.active }
    }
}
