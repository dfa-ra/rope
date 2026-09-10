package app.rope.android.data

/**
 * Telegram-like in-thread unread separator. Watermark is local
 * ([ChatPrefs.unread] / [ChatPrefs.lastReadMs]); Go never sees it.
 */
object UnreadSeparatorRules {
    const val KEY = "unread"
    const val LABEL = "Непрочитанные сообщения"

    fun firstUnreadId(
        messages: List<ChatMessage>,
        unreadCount: Int,
        lastReadMs: Long = 0L,
    ): String? {
        if (unreadCount <= 0) return null
        val incoming = messages
            .filter { !it.outgoing && !it.deleted }
            .sortedWith(compareBy<ChatMessage> { it.timestampMs }.thenBy { it.id })
        if (incoming.isEmpty()) return null
        val afterWatermark = incoming.filter { it.timestampMs > lastReadMs }
        val pool = if (lastReadMs > 0L && afterWatermark.isNotEmpty()) {
            afterWatermark
        } else {
            incoming.takeLast(unreadCount)
        }
        return pool.firstOrNull()?.id
    }

    fun insert(
        items: List<ChatThreadItem>,
        firstUnreadId: String?,
        searching: Boolean = false,
    ): List<ChatThreadItem> {
        val id = firstUnreadId?.takeIf { it.isNotBlank() } ?: return items
        if (searching) return items
        if (items.any { it is ChatThreadItem.Unread }) return items
        val idx = AlbumRules.indexOfMessage(items, id)
        if (idx < 0) return items
        return items.take(idx) + ChatThreadItem.Unread + items.drop(idx)
    }

    fun scrollIndex(
        items: List<ChatThreadItem>,
        scrollId: String?,
        unreadAnchorId: String?,
    ): Int {
        val id = scrollId?.takeIf { it.isNotBlank() } ?: return -1
        if (id == unreadAnchorId || id == KEY) {
            val unread = items.indexOfFirst { it is ChatThreadItem.Unread }
            if (unread >= 0) return unread
        }
        return AlbumRules.indexOfMessage(items, id)
    }

    fun fab(
        atBottom: Boolean,
        unreadVisible: Boolean,
        hasUnread: Boolean,
        unreadAbove: Boolean = false,
    ): UnreadFab? = when {
        hasUnread && !unreadVisible && unreadAbove -> UnreadFab.UP
        !atBottom -> UnreadFab.DOWN
        else -> null
    }

    fun chatId(groupId: String?, peerDeviceId: String?): String? {
        JsonIds.optional(groupId)?.let { return ChatIds.group(it) }
        return JsonIds.optional(peerDeviceId)
    }

    fun unreadOf(conversations: List<Conversation>, chatId: String?): Int {
        val id = chatId ?: return 0
        return conversations.firstOrNull { it.id == id }?.unread?.coerceAtLeast(0) ?: 0
    }

    /** Count on the jump FAB. Empty when there is nothing unread. */
    fun fabBadge(kind: UnreadFab?, unread: Int): String {
        if (kind == null) return ""
        return UnreadBadgeRules.label(unread)
    }

    fun fabContentDescription(kind: UnreadFab, badge: String): String {
        val base = if (kind == UnreadFab.UP) "К непрочитанным" else "К последним"
        return if (badge.isBlank()) base else "$base, $badge"
    }
}

enum class UnreadFab {
    UP,
    DOWN,
}
