package app.rope.android.data

/**
 * Mark as read from the notification shade. Not shade reply, not FCM.
 * Chat id rejects CR/LF/NUL. LocalStore stays v6.
 */
object NotifReadRules {
    const val ACTION = "app.rope.android.NOTIFY_READ"
    const val EXTRA_CHAT_ID = "app.rope.android.extra.CHAT_ID"
    const val EXTRA_NOTIFY_ID = "app.rope.android.extra.NOTIFY_ID"
    const val LABEL = "Прочитать"
    const val MAX_ID = 128

    fun chatId(raw: String?): String? {
        if (raw == null) return null
        if (hasCtl(raw)) return null
        val id = JsonIds.optional(raw) ?: return null
        if (hasCtl(id) || id.length > MAX_ID) return null
        return id
    }

    fun allows(chatId: String?): Boolean {
        val id = chatId(chatId) ?: return false
        return !SavedMessagesRules.isSaved(id)
    }

    fun apply(prefs: ChatPrefs, nowMs: Long): ChatPrefs =
        prefs.copy(unread = 0, lastReadMs = nowMs)

    fun requestCode(chatId: String): Int {
        val id = chatId(chatId) ?: return 0x52454144
        return 0x52454144 xor id.hashCode()
    }

    private fun hasCtl(s: String): Boolean =
        s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0 || s.indexOf('\u0000') >= 0
}
