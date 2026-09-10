package app.rope.android.data

/**
 * Telegram-like local delete chat. kv `chat_prefs.deleted` plus a hard wipe
 * of that peer's messages — no Go, no DeviceBackup, no LocalStore bump.
 * A new incoming message (or opening the person again) restores the row.
 */
object DeleteChatRules {
    const val ACTION = "Удалить чат"
    const val CONFIRM = "Точно удалить"
    const val BODY = "История сотрётся только на этом телефоне."

    fun canDelete(id: String?): Boolean =
        !id.isNullOrBlank() && !SavedMessagesRules.isSaved(id)

    fun canDelete(c: Conversation): Boolean = canDelete(c.id)

    fun shouldHide(c: Conversation): Boolean =
        c.deleted && !SavedMessagesRules.isSaved(c.id)

    fun visibleOf(conversations: List<Conversation>): List<Conversation> =
        conversations.filter { !shouldHide(it) }

    fun prompt(title: String): String {
        val name = title.trim()
        return if (name.isEmpty()) "Удалить чат?" else "Удалить чат «$name»?"
    }

    /** Wipe pin/mute/archive/draft/unread and hide the row. */
    fun clearedPrefs(): ChatPrefs = ChatPrefs(deleted = true)

    fun restorePrefs(cur: ChatPrefs): ChatPrefs =
        if (cur.deleted) cur.copy(deleted = false) else cur

    fun leaveOpenChat(openId: String?, deletedId: String): Boolean =
        !deletedId.isBlank() && openId == deletedId
}
