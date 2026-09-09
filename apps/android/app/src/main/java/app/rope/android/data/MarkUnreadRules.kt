package app.rope.android.data

/**
 * Telegram-like chat-list long-press: mark unread (badge 1) or mark read
 * without opening the thread. Saved Messages is out. ChatPrefs JSON only —
 * LocalStore stays at v6.
 */
object MarkUnreadRules {
    const val MARK_UNREAD = "Пометить непрочитанным"
    const val MARK_READ = "Пометить прочитанным"
    const val UNREAD_COUNT = 1

    fun canMark(id: String?): Boolean =
        !id.isNullOrBlank() && !SavedMessagesRules.isSaved(id)

    fun canMark(c: Conversation): Boolean = canMark(c.id)

    fun isUnread(unread: Int): Boolean = unread > 0

    fun label(unread: Int): String = if (isUnread(unread)) MARK_READ else MARK_UNREAD

    fun markUnread(cur: ChatPrefs): ChatPrefs = cur.copy(unread = UNREAD_COUNT)

    fun markRead(cur: ChatPrefs, nowMs: Long): ChatPrefs =
        cur.copy(unread = 0, lastReadMs = nowMs)

    fun apply(cur: ChatPrefs, nowMs: Long): ChatPrefs =
        if (isUnread(cur.unread)) markRead(cur, nowMs) else markUnread(cur)
}
