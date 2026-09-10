package app.rope.android.data

/**
 * Pin/unpin from 1:1 profile and group info. Same kv `chat_prefs.pinned`
 * as the chat-list long-press. Archived chats stay unpinnable.
 * No LocalStore bump.
 */
object PinChatRules {
    const val PIN = "Закрепить"
    const val UNPIN = "Открепить"

    fun canPin(id: String?, conversations: List<Conversation>): Boolean {
        if (id.isNullOrBlank()) return false
        val row = conversations.find { it.id == id } ?: return true
        return ArchiveRules.canPin(row)
    }

    fun isPinned(conversations: List<Conversation>, id: String?): Boolean =
        !id.isNullOrBlank() && conversations.any { it.id == id && it.pinned }

    fun profileAction(pinned: Boolean): String = if (pinned) UNPIN else PIN
}
