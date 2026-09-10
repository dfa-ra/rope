package app.rope.android.data

/**
 * Pin a chat to the Android home screen. Long-press → «На главный экран»
 * asks the launcher to add a shortcut that reopens that thread.
 * Reject CR/LF/NUL. LocalStore stays v6. Not FCM.
 */
object ChatHomeRules {
    const val LABEL = "На главный экран"
    const val ACTION = "app.rope.android.OPEN_CHAT"
    const val EXTRA_CHAT_ID = "app.rope.android.extra.CHAT_ID"
    const val SHORTCUT_PREFIX = "rope-chat:"
    const val MAX_ID = 128
    const val FALLBACK_TITLE = "Чат"

    fun show(forwarding: Boolean): Boolean = !forwarding

    fun sanitizeId(raw: String?): String? {
        if (raw == null) return null
        if (raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0 || raw.indexOf('\u0000') >= 0) {
            return null
        }
        val id = raw.trim()
        if (id.isEmpty() || id.length > MAX_ID) return null
        if (id.any { ch -> ch != ':' && ch != '-' && !ch.isLetterOrDigit() }) return null
        return id
    }

    fun canPin(id: String?): Boolean = sanitizeId(id) != null

    fun shortcutId(chatId: String): String = SHORTCUT_PREFIX + chatId

    fun shortLabel(title: String): String {
        val t = title.trim().ifBlank { FALLBACK_TITLE }
        return if (t.length <= 12) t else t.take(11) + "…"
    }

    fun longLabel(title: String): String {
        val t = title.trim().ifBlank { FALLBACK_TITLE }
        return if (t.length <= 30) t else t.take(29) + "…"
    }

    fun consume(action: String?, extra: String?): String? {
        if (action != ACTION) return null
        return sanitizeId(extra)
    }

    fun matches(chatId: String, candidate: String): Boolean {
        if (chatId == candidate) return true
        if (ChatIds.isSaved(chatId) || ChatIds.isGroup(chatId)) return false
        return PeerIds.same(chatId, candidate)
    }
}
