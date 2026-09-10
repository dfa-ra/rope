package app.rope.android.data

/**
 * Tap a message notification to open that chat. Chat id rides an explicit
 * MainActivity extra — not FCM, not a join deep link. Reject CR/LF/NUL.
 * LocalStore stays v6.
 */
object NotifyChatRules {
    const val EXTRA_CHAT_ID = "app.rope.android.extra.CHAT_ID"
    const val URI_SCHEME = "rope-notify"
    const val URI_HOST = "chat"
    const val MAX_ID = 128

    fun chatId(raw: String?): String? {
        if (raw == null) return null
        if (hasCtl(raw)) return null
        val id = JsonIds.optional(raw) ?: return null
        if (hasCtl(id) || id.length > MAX_ID) return null
        return id
    }

    fun requestCode(chatId: String): Int {
        val id = chatId(chatId) ?: return 0x4E0F10
        return 0x4E0F10 xor id.hashCode()
    }

    fun tapUri(chatId: String): String? {
        val id = chatId(chatId) ?: return null
        return "$URI_SCHEME://$URI_HOST/$id"
    }

    fun canOpen(signedIn: Boolean, incoming: String?): Boolean =
        signedIn && chatId(incoming) != null

    fun alreadyShowing(openChatId: String?, incoming: String): Boolean {
        val id = chatId(incoming) ?: return false
        return PeerIds.same(openChatId, id)
    }

    fun parentChats(onChat: Boolean, onChats: Boolean, onArchive: Boolean): Boolean =
        !onChat && !onChats && !onArchive

    private fun hasCtl(s: String): Boolean =
        s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0 || s.indexOf('\u0000') >= 0
}
