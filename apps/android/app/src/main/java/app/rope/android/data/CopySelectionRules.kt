package app.rope.android.data

/**
 * Telegram-like copy of one or many selected messages. A single row pastes
 * as the text only; several rows include the sender name. LocalStore v6.
 */
object CopySelectionRules {
    const val NOTICE = "Скопировано"
    const val YOU = GroupChatUx.YOU

    fun payload(msg: ChatMessage): String? {
        if (msg.deleted) return null
        val t = msg.text.trim()
        return t.takeIf { it.isNotEmpty() }
    }

    fun canCopy(messages: List<ChatMessage>): Boolean = messages.any { payload(it) != null }

    fun senderLabel(msg: ChatMessage, peerFallback: String): String {
        if (msg.outgoing) return YOU
        val named = msg.senderName.trim()
        if (named.isNotEmpty()) return named
        val peer = peerFallback.trim()
        return peer.ifEmpty { "…" }
    }

    fun join(messages: List<ChatMessage>, peerFallback: String): String {
        val parts = messages.mapNotNull { m ->
            val body = payload(m) ?: return@mapNotNull null
            m to body
        }
        if (parts.isEmpty()) return ""
        if (messages.size == 1) return parts.single().second
        return parts.joinToString("\n\n") { (m, body) ->
            "${senderLabel(m, peerFallback)}:\n$body"
        }
    }
}
