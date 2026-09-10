package app.rope.android.data

/**
 * Telegram-like attributed forwards. The origin name lives inside the
 * encrypted payload (`ff`), not as a fake reply quote «Переслано ·».
 */
object ForwardRules {
    const val HEADER = "Переслано от"
    const val LEGACY_PREFIX = "Переслано ·"

    fun originName(src: ChatMessage, fallback: String): String {
        val from = originField(src.senderName).orEmpty()
        val fb = originField(fallback).orEmpty()
        return from.ifBlank { fb }.ifBlank { "сообщение" }
    }

    fun attributedName(msg: ChatMessage): String? {
        originField(msg.forwardedFrom)?.let { return it }
        val rn = originField(msg.replyName) ?: return null
        if (rn.startsWith(LEGACY_PREFIX)) {
            return originField(rn.removePrefix(LEGACY_PREFIX))
        }
        return null
    }

    fun hidesReplyQuote(msg: ChatMessage): Boolean {
        if (!originField(msg.forwardedFrom).isNullOrBlank()) {
            return msg.replyToId.isNullOrBlank()
        }
        val rn = originField(msg.replyName) ?: return false
        return attributedName(msg) != null && rn.startsWith(LEGACY_PREFIX)
    }

    fun headerLabel(name: String): String {
        val who = originField(name) ?: return HEADER
        return "$HEADER $who"
    }

    fun isMasqueradingReply(replyName: String): Boolean {
        val rn = originField(replyName) ?: return false
        return rn.startsWith(LEGACY_PREFIX)
    }

    /**
     * Fail closed on CR/LF/NUL before trim so a newline prefix cannot become
     * a live «Переслано от» name or a masquerading reply quote. Spaces still
     * trim. Caption and text keep newlines.
     */
    internal fun originField(raw: String?): String? {
        if (raw.isNullOrEmpty()) return null
        if (raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0 || raw.indexOf('\u0000') >= 0) {
            return null
        }
        return JsonIds.optional(raw)
    }
}
