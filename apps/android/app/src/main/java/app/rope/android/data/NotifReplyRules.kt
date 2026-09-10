package app.rope.android.data

/**
 * Reply from the notification shade via RemoteInput. Not tap-to-open,
 * not FCM. Chat id and body reject CR/LF/NUL. LocalStore stays v6.
 */
object NotifReplyRules {
    const val ACTION = "app.rope.android.NOTIFY_REPLY"
    const val EXTRA_CHAT_ID = "app.rope.android.extra.CHAT_ID"
    const val EXTRA_NOTIFY_ID = "app.rope.android.extra.NOTIFY_ID"
    const val REMOTE_KEY = "app.rope.android.remote.REPLY"
    const val LABEL = "Ответить"
    const val MAX_ID = 128
    const val MAX_TEXT = 4096

    fun chatId(raw: String?): String? {
        if (raw == null) return null
        if (hasCtl(raw)) return null
        val id = JsonIds.optional(raw) ?: return null
        if (hasCtl(id) || id.length > MAX_ID) return null
        return id
    }

    fun text(raw: String?): String? {
        if (raw == null) return null
        if (hasCtl(raw)) return null
        val t = raw.trim()
        if (t.isEmpty()) return null
        return if (t.length <= MAX_TEXT) t else t.take(MAX_TEXT)
    }

    fun allowsReply(chatId: String?): Boolean {
        val id = chatId(chatId) ?: return false
        return !SavedMessagesRules.isSaved(id)
    }

    fun requestCode(chatId: String): Int {
        val id = chatId(chatId) ?: return 0x4E0F21
        return 0x4E0F21 xor id.hashCode()
    }

    private fun hasCtl(s: String): Boolean =
        s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0 || s.indexOf('\u0000') >= 0
}
