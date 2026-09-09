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
}
