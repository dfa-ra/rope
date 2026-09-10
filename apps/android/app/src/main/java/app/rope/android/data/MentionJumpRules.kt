package app.rope.android.data

/**
 * Telegram-like in-thread jump to the next unread @mention of you.
 * Groups only. Kv unread watermark stays on ChatPrefs; LocalStore v6.
 */
object MentionJumpRules {
    const val MARK = "@"
    const val LABEL = "К упоминанию"

    val BROADCAST = listOf("всем", "все", "all", "everyone")

    fun namesForMe(displayName: String): List<String> {
        val out = mutableListOf<String>()
        val name = displayName.trim()
        if (name.isNotEmpty() && !hasCtl(name)) out += name
        out += GroupChatUx.YOU
        out += BROADCAST
        return out.distinct()
    }

    fun bodyOf(msg: ChatMessage): String {
        val cap = MediaSendRules.captionOf(msg).orEmpty()
        return listOf(msg.text, cap).filter { it.isNotBlank() }.joinToString("\n")
    }

    fun hits(msg: ChatMessage, myName: String): Boolean {
        if (msg.outgoing || msg.deleted) return false
        return GroupChatUx.mentionSpans(bodyOf(msg), namesForMe(myName)).isNotEmpty()
    }

    fun unreadIds(
        messages: List<ChatMessage>,
        myName: String,
        unreadAnchorId: String?,
        isGroup: Boolean,
    ): List<String> {
        if (!isGroup) return emptyList()
        val anchor = unreadAnchorId?.takeIf { it.isNotBlank() } ?: return emptyList()
        val fromTs = messages.firstOrNull { it.id == anchor }?.timestampMs ?: return emptyList()
        return messages
            .filter { it.timestampMs >= fromTs && hits(it, myName) }
            .map { it.id }
    }

    fun nextId(ids: List<String>, afterId: String?): String? {
        if (ids.isEmpty()) return null
        if (afterId.isNullOrBlank()) return ids.first()
        val i = ids.indexOf(afterId)
        if (i < 0) return ids.first()
        return ids.getOrElse(i + 1) { ids.first() }
    }

    fun showFab(ids: List<String>): Boolean = ids.isNotEmpty()

    private fun hasCtl(s: String): Boolean =
        s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0 || s.indexOf('\u0000') >= 0
}
