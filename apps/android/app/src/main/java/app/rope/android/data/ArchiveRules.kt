package app.rope.android.data

/**
 * Local Telegram-like chat archive. kv `chat_prefs.archived` only — no Go,
 * no DeviceBackup, no LocalStore bump, not a custom folder.
 */
object ArchiveRules {
    const val TITLE = "Архив"
    const val SEARCH_PLACEHOLDER = "Поиск в архиве"
    const val EMPTY_TITLE = "Архив пуст"
    const val DEVICE_ONLY = "Архив только на этом телефоне."
    const val ARCHIVE = "В архив"
    const val UNARCHIVE = "Разархивировать"
    const val COUNT_SUFFIX = "чатов"

    fun canArchive(id: String?): Boolean = !SavedMessagesRules.isSaved(id)

    fun shouldHideFromMain(c: Conversation): Boolean =
        c.archived && !SavedMessagesRules.isSaved(c.id)

    /** Always unpin on archive so a hidden chat cannot stay pinned. */
    fun unpinOnArchive(pinned: Boolean): Boolean = true

    /** Archived chats cannot be pinned. Saved still can (it is never archived). */
    fun canPin(prefs: ChatPrefs): Boolean = !prefs.archived

    fun canPin(c: Conversation): Boolean = !c.archived

    fun rowVisible(archivedCount: Int, mode: ChatListMode, forwarding: Boolean): Boolean =
        archivedCount > 0 && mode == ChatListMode.ALL && !forwarding

    fun unreadSum(archived: List<Conversation>): Int =
        archived.sumOf { it.unread.coerceAtLeast(0) }

    fun badgeKind(archived: List<Conversation>): UnreadBadgeKind {
        val unread = archived.filter { it.unread > 0 }
        if (unread.isEmpty()) return UnreadBadgeKind.NONE
        if (unread.any { !it.muted }) return UnreadBadgeKind.ACCENT
        return UnreadBadgeKind.MUTED
    }

    fun preview(archived: List<Conversation>): String {
        if (archived.isEmpty()) return ""
        val latest = archived.maxByOrNull { it.last?.timestampMs ?: 0L }
        val line = latest?.subtitle?.trim().orEmpty()
        if (latest?.last != null && line.isNotEmpty()) return line
        return "${archived.size} $COUNT_SUFFIX"
    }

    fun archivePrefs(cur: ChatPrefs): ChatPrefs =
        cur.copy(archived = true, pinned = false)

    fun unarchivePrefs(cur: ChatPrefs): ChatPrefs =
        cur.copy(archived = false)

    /**
     * List pipeline: hide archived on All/Groups/Calls; Archive screen sees
     * only archived; forward picker keeps them. Folders (sibling #65) can
     * later run `FolderRules.apply` on the All result without seeing archived.
     */
    fun sourceForList(
        conversations: List<Conversation>,
        mode: ChatListMode,
        forwarding: Boolean,
    ): List<Conversation> = when {
        forwarding -> conversations
        mode == ChatListMode.ARCHIVE -> conversations.filter { shouldHideFromMain(it) }
        else -> conversations.filter { !shouldHideFromMain(it) }
    }

    fun archivedOf(conversations: List<Conversation>): List<Conversation> =
        conversations.filter { shouldHideFromMain(it) }
}
