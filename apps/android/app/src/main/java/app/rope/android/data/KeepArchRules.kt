package app.rope.android.data

/**
 * Settings «Не доставать из архива»: incoming mail stays in Archive
 * unless the toggle is off. Default on. Not envelope crypto.
 */
object KeepArchRules {
    fun enabledFromKv(raw: String?): Boolean = raw != "0"

    fun persist(enabled: Boolean): String = if (enabled) "1" else "0"

    fun hint(): String =
        "Новое сообщение не вытаскивает чат из архива. По умолчанию включено."

    /**
     * Hidden incoming mail increments unread. When [keepArchived] is off,
     * the chat also returns to the main list.
     */
    fun nextPrefs(cur: ChatPrefs, keepArchived: Boolean): ChatPrefs {
        val unread = cur.copy(unread = cur.unread + 1)
        if (!unread.archived || keepArchived) return unread
        return ArchiveRules.unarchivePrefs(unread)
    }
}
