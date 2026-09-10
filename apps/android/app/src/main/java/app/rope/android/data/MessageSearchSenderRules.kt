package app.rope.android.data

data class SearchSender(
    val id: String,
    val label: String,
)

/**
 * Group in-chat «От кого» filter. Distinct senders from the open thread.
 * Not People search, not media-hub.
 */
object MessageSearchSenderRules {
    const val CHIP = "От кого"
    const val EMPTY_FALLBACK = "Нет сообщений от этого отправителя."

    fun idOf(msg: ChatMessage, myId: String): String {
        val sid = msg.senderId.trim()
        if (sid.isNotEmpty()) return sid
        if (msg.outgoing) return myId.trim().ifBlank { "me" }
        return ""
    }

    fun labelOf(msg: ChatMessage, myId: String): String {
        val id = idOf(msg, myId)
        if (id.isNotEmpty() && (msg.outgoing || (myId.isNotBlank() && id == myId))) return GroupChatUx.YOU
        return msg.senderName.trim().ifBlank { id.take(8).ifBlank { CHIP } }
    }

    fun chips(messages: List<ChatMessage>, myId: String): List<SearchSender> {
        val seen = linkedMapOf<String, String>()
        for (msg in messages) {
            val id = idOf(msg, myId)
            if (id.isBlank()) continue
            if (id !in seen) seen[id] = labelOf(msg, myId)
        }
        return seen.map { SearchSender(it.key, it.value) }
    }

    fun matches(msg: ChatMessage, senderId: String?, myId: String): Boolean {
        if (senderId.isNullOrBlank()) return true
        return idOf(msg, myId) == senderId
    }

    fun toggle(current: String?, tapped: String): String? =
        if (current == tapped) null else tapped

    fun searching(query: String, senderId: String?): Boolean =
        ChatListRules.searching(query) || !senderId.isNullOrBlank()

    fun emptyBody(senderLabel: String): String {
        val name = senderLabel.trim().ifBlank { CHIP }
        val full = "Нет сообщений от $name в этом чате."
        return if (full.length <= ThreadEmptyRules.COPY_MAX) full else EMPTY_FALLBACK
    }
}
