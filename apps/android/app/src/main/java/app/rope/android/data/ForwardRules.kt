package app.rope.android.data

/**
 * Telegram-like attributed forwards. The origin name lives inside the
 * encrypted payload (`ff`), not as a fake reply quote «Переслано ·».
 */
object ForwardRules {
    const val HEADER = "Переслано от"
    const val LEGACY_PREFIX = "Переслано ·"

    fun originName(src: ChatMessage, fallback: String): String =
        src.senderName.ifBlank { fallback }.ifBlank { "сообщение" }

    fun attributedName(msg: ChatMessage): String? {
        JsonIds.optional(msg.forwardedFrom)?.let { return it }
        val rn = msg.replyName.trim()
        if (rn.startsWith(LEGACY_PREFIX)) {
            return rn.removePrefix(LEGACY_PREFIX).trim().ifBlank { null }
        }
        return null
    }

    fun hidesReplyQuote(msg: ChatMessage): Boolean {
        if (!JsonIds.optional(msg.forwardedFrom).isNullOrBlank()) {
            return msg.replyToId.isNullOrBlank()
        }
        return attributedName(msg) != null && msg.replyName.trim().startsWith(LEGACY_PREFIX)
    }

    fun headerLabel(name: String): String = "$HEADER $name"

    fun isMasqueradingReply(replyName: String): Boolean =
        replyName.trim().startsWith(LEGACY_PREFIX)

    fun pick(msgs: List<ChatMessage>): List<ChatMessage> =
        msgs.filter { ChatActions.canForward(it) }

    fun active(msgs: List<ChatMessage>): Boolean = msgs.isNotEmpty()

    fun banner(msgs: List<ChatMessage>): String = when (msgs.size) {
        0 -> ""
        1 -> msgs.first().preview()
        else -> countLabel(msgs.size)
    }

    fun countLabel(n: Int): String {
        val abs = n.coerceAtLeast(0)
        val n10 = abs % 10
        val n100 = abs % 100
        val noun = when {
            n10 == 1 && n100 != 11 -> "сообщение"
            n10 in 2..4 && n100 !in 12..14 -> "сообщения"
            else -> "сообщений"
        }
        return "$abs $noun"
    }
}
