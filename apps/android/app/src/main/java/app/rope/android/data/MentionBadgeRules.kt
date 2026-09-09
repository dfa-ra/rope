package app.rope.android.data

/**
 * Telegram-like unread @ badge on group chats. Counts live in ChatPrefs JSON
 * (`unread_mentions`); LocalStore schema stays v6. Go never sees it.
 */
object MentionBadgeRules {
    const val MARK = "@"
    const val LABEL = "Упоминание"

    val BROADCAST = listOf("all", "everyone", "все", "всем")

    fun namesForMe(displayName: String): List<String> =
        listOf(displayName.trim()).filter { it.isNotEmpty() } + BROADCAST

    fun mentionsMe(text: String, myName: String): Boolean =
        GroupChatUx.mentionSpans(text, namesForMe(myName)).isNotEmpty()

    fun hit(isGroup: Boolean, text: String, myName: String): Boolean =
        isGroup && mentionsMe(text, myName)

    fun nextCount(current: Int, hit: Boolean): Int =
        if (hit) (current + 1).coerceAtLeast(1) else current.coerceAtLeast(0)

    fun afterHidden(cur: ChatPrefs, isGroup: Boolean, body: String, myName: String): ChatPrefs =
        cur.copy(
            unread = cur.unread + 1,
            unreadMentions = nextCount(cur.unreadMentions, hit(isGroup, body, myName)),
        )

    fun afterOpened(cur: ChatPrefs, nowMs: Long): ChatPrefs =
        cur.copy(unread = 0, lastReadMs = nowMs, unreadMentions = 0)

    fun showMark(mentioned: Boolean): Boolean = mentioned

    fun mentioned(unreadMentions: Int): Boolean = unreadMentions > 0
}
