package app.rope.android.data

/**
 * Telegram-like autoplay of the next voice note in the same thread after
 * the current clip reaches the end. Pause and stop do not advance.
 * Video notes stay separate. LocalStore stays v6.
 */
object VoiceNextRules {
    fun eligible(msg: ChatMessage): Boolean {
        if (msg.deleted) return false
        if (msg.kind != MessageKind.VOICE) return false
        return !msg.localPath.isNullOrBlank()
    }

    fun next(messages: List<ChatMessage>, currentId: String): ChatMessage? {
        if (currentId.isBlank()) return null
        val ordered = messages.sortedBy { it.timestampMs }
        val i = ordered.indexOfFirst { it.id == currentId }
        if (i < 0) return null
        return ordered.drop(i + 1).firstOrNull { eligible(it) }
    }
}
