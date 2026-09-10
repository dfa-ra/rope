package app.rope.android.data

/**
 * Shade **Без звука** sets [ChatPrefs.muted]. Not mark-read, not reply, not
 * global notifications mute, not FCM. Chat id rejects CR/LF/NUL. LocalStore stays v6.
 */
object NotifMuteRules {
    const val ACTION = "app.rope.android.NOTIFY_MUTE"
    const val EXTRA_CHAT_ID = "app.rope.android.extra.MUTE_CHAT_ID"
    const val EXTRA_NOTIFY_ID = "app.rope.android.extra.MUTE_NOTIFY_ID"
    const val LABEL = "Без звука"
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

    fun apply(prefs: ChatPrefs): ChatPrefs = prefs.copy(muted = true)

    fun requestCode(chatId: String): Int {
        val id = chatId(chatId) ?: return 0x4D555445
        return 0x4D555445 xor id.hashCode()
    }

    private fun hasCtl(s: String): Boolean =
        s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0 || s.indexOf('\u0000') >= 0
}
