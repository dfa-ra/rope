package app.rope.android.data

/**
 * Telegram-like «Прочитать все» on the conversation list. Same kv
 * `chat_prefs.unread` / `last_read_ms` as opening a chat. No LocalStore bump.
 */
object MarkAllReadRules {
    const val ACTION = "Прочитать все"

    fun canMark(id: String?): Boolean =
        !id.isNullOrBlank() && !SavedMessagesRules.isSaved(id)

    fun ids(conversations: List<Conversation>): List<String> =
        conversations.filter { it.unread > 0 && canMark(it.id) }.map { it.id }

    fun visible(unreadCount: Int, forwarding: Boolean, searching: Boolean): Boolean =
        unreadCount > 0 && !forwarding && !searching

    fun prefsAfter(cur: ChatPrefs, nowMs: Long): ChatPrefs =
        cur.copy(unread = 0, lastReadMs = nowMs)
}
