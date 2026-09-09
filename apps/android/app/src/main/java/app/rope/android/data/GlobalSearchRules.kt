package app.rope.android.data

/**
 * Local Telegram-like global message search. Reads decrypted LocalStore rows
 * only — no Go, no schema bump, not a custom folder.
 */
data class GlobalSearchHit(
    val chatId: String,
    val messageId: String,
    val title: String,
    val snippet: String,
    val timestampMs: Long,
    val isGroup: Boolean,
)

object GlobalSearchRules {
    const val SECTION = "Сообщения"
    const val MIN_CHARS = 2
    const val MAX_HITS = 40
    const val SCAN_LIMIT = 800

    fun shouldSearch(query: String, forwarding: Boolean, mode: ChatListMode): Boolean =
        !forwarding && mode == ChatListMode.ALL && ChatListRules.normalize(query).length >= MIN_CHARS

    fun matches(msg: ChatMessage, query: String): Boolean {
        if (msg.deleted || msg.id.isBlank() || msg.peerDeviceId.isBlank()) return false
        val q = ChatListRules.normalize(query)
        if (q.length < MIN_CHARS) return false
        return haystack(msg).contains(q)
    }

    /**
     * Left groups are not leftover threads ([ChatRouting.showLeftoverThread]
     * is false for `g:`) and have no read-only view. Skip them so a hit
     * cannot appear without an openable chat.
     */
    fun canOpen(chatId: String, openableChatIds: Set<String>): Boolean {
        if (chatId.isBlank()) return false
        if (SavedMessagesRules.isSaved(chatId)) return true
        if (ChatIds.isGroup(chatId)) return chatId in openableChatIds
        return true
    }

    fun snippet(msg: ChatMessage): String {
        val raw = if (msg.text.isNotBlank()) msg.text else msg.preview()
        return raw.replace('\n', ' ').replace(Regex("\\s+"), " ").trim().take(120)
    }

    fun hits(
        messages: List<ChatMessage>,
        titles: Map<String, String>,
        query: String,
        openableChatIds: Set<String> = emptySet(),
    ): List<GlobalSearchHit> {
        if (ChatListRules.normalize(query).length < MIN_CHARS) return emptyList()
        val seen = HashSet<String>()
        return messages
            .asSequence()
            .filter { matches(it, query) }
            .filter { canOpen(it.peerDeviceId, openableChatIds) }
            .sortedWith(compareByDescending<ChatMessage> { it.timestampMs }.thenBy { it.id })
            .mapNotNull { msg ->
                if (!seen.add(msg.id)) return@mapNotNull null
                val title = titles[msg.peerDeviceId]?.trim().orEmpty()
                    .ifBlank { msg.peerDeviceId.take(8) }
                GlobalSearchHit(
                    chatId = msg.peerDeviceId,
                    messageId = msg.id,
                    title = title,
                    snippet = snippet(msg),
                    timestampMs = msg.timestampMs,
                    isGroup = ChatIds.isGroup(msg.peerDeviceId),
                )
            }
            .take(MAX_HITS)
            .toList()
    }

    private fun haystack(msg: ChatMessage): String = buildString {
        if (msg.text.isNotBlank()) append(msg.text.lowercase()).append('\n')
        val preview = msg.preview()
        if (preview.isNotBlank() && preview != "Сообщение удалено") {
            append(preview.lowercase()).append('\n')
        }
        if (msg.senderName.isNotBlank()) append(msg.senderName.lowercase())
    }
}
