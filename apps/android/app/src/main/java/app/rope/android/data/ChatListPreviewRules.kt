package app.rope.android.data

enum class ChatListPreviewKind { DRAFT, LAST, PRESENCE }

data class ChatListPreviewCopy(
    val text: String,
    val kind: ChatListPreviewKind,
)

/**
 * Telegram-like chat-list last-message line: an unsent draft beats the last
 * message («Черновик: …»); DM outgoing uses «Вы:»; groups keep sender-colon
 * preview. Presence / member-count is only the idle fallback.
 */
object ChatListPreviewRules {
    const val DRAFT_LABEL = "Черновик"
    const val YOU = GroupChatUx.YOU
    const val BODY_MAX = 80
    const val ONLINE = "в сети"
    const val OFFLINE = "не в сети"

    private val whitespace = Regex("\\s+")

    fun copy(
        last: ChatMessage?,
        draft: String,
        isGroup: Boolean,
        myDeviceId: String = "",
        online: Boolean = false,
        memberCount: Int = 0,
        saved: Boolean = false,
    ): ChatListPreviewCopy {
        val clippedDraft = clip(draft)
        if (clippedDraft.isNotEmpty()) {
            return ChatListPreviewCopy("$DRAFT_LABEL: $clippedDraft", ChatListPreviewKind.DRAFT)
        }
        if (last != null) {
            val body = when {
                saved -> TextFmtRules.plain(last.preview())
                isGroup -> GroupChatUx.listPreview(last, myDeviceId) ?: TextFmtRules.plain(last.preview())
                else -> dmLast(last, myDeviceId)
            }
            return ChatListPreviewCopy(body, ChatListPreviewKind.LAST)
        }
        if (saved) {
            return ChatListPreviewCopy(SavedMessagesRules.IDLE_SUBTITLE, ChatListPreviewKind.PRESENCE)
        }
        if (isGroup) {
            return ChatListPreviewCopy("$memberCount участников", ChatListPreviewKind.PRESENCE)
        }
        return ChatListPreviewCopy(if (online) ONLINE else OFFLINE, ChatListPreviewKind.PRESENCE)
    }

    fun dmLast(last: ChatMessage, myDeviceId: String): String {
        val body = TextFmtRules.plain(last.preview())
        val mine = last.outgoing || (myDeviceId.isNotBlank() && last.senderId == myDeviceId)
        return if (mine) "$YOU: $body" else body
    }

    fun isDraft(text: String): Boolean = text.startsWith("$DRAFT_LABEL: ")

    fun clip(raw: String): String {
        val one = TextFmtRules.plain(raw).replace(whitespace, " ").trim()
        if (one.length <= BODY_MAX) return one
        return one.take(BODY_MAX).trimEnd()
    }
}
