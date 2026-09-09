package app.rope.android.data

/**
 * Telegram-like pinned-message bar at the top of a 1:1 or group thread.
 * Hide (X) is local chrome — it does not unpin. Pin/unpin stays on the
 * message menu + RECEIPT. Persistence is [ChatPrefs] JSON, not a LocalStore bump.
 */
data class PinnedBarCopy(
    val title: String,
    val body: String,
    val jumpId: String,
    val jumpContentDescription: String,
    val dismissContentDescription: String,
)

object PinnedBarRules {
    const val TITLE = "Закреплённое сообщение"
    const val FALLBACK_BODY = "Сообщение"
    const val DELETED = "Сообщение удалено"
    const val BODY_MAX = 80
    const val DISMISS = "Скрыть закреплённое"
    const val JUMP = "Перейти к закреплённому"

    fun message(messages: List<ChatMessage>, pinnedId: String?): ChatMessage? {
        val id = JsonIds.optional(pinnedId) ?: return null
        return messages.firstOrNull { it.id == id && !it.deleted }
    }

    fun visible(message: ChatMessage?, hiddenId: String?): Boolean {
        if (message == null || message.deleted || message.id.isBlank()) return false
        return !isHidden(message.id, hiddenId)
    }

    fun isHidden(pinnedId: String?, hiddenId: String?): Boolean {
        val pin = JsonIds.optional(pinnedId) ?: return false
        return JsonIds.optional(hiddenId) == pin
    }

    /** X on the bar hides it; the message stays pinned. */
    fun hide(pinnedId: String?): String? = JsonIds.optional(pinnedId)

    /**
     * Pin id changed. Unpin or a different pin unhides; a duplicate SET of the
     * same id keeps the local hide. ChatPrefs JSON only — not LocalStore v7/v8.
     */
    fun hiddenAfterPinChange(newPinnedId: String?, previousHidden: String? = null): String? {
        val pin = JsonIds.optional(newPinnedId) ?: return null
        val hidden = JsonIds.optional(previousHidden) ?: return null
        return hidden.takeIf { it == pin }
    }

    fun jumpId(message: ChatMessage?): String? {
        if (message == null || message.deleted) return null
        return JsonIds.optional(message.id)
    }

    fun copy(message: ChatMessage): PinnedBarCopy = PinnedBarCopy(
        title = TITLE,
        body = body(message),
        jumpId = message.id,
        jumpContentDescription = JUMP,
        dismissContentDescription = DISMISS,
    )

    fun body(message: ChatMessage): String {
        if (message.deleted) return DELETED
        val media = mediaPreview(message)
        val raw = when {
            message.text.isNotBlank() -> message.text
            !media.isNullOrBlank() -> media
            else -> message.preview()
        }
        return clip(raw)
    }

    fun clip(preview: String): String {
        val one = preview.replace(WHITESPACE, " ").trim()
        if (one.isEmpty()) return FALLBACK_BODY
        if (one.length <= BODY_MAX) return one
        return one.take(BODY_MAX - 1).trimEnd() + "…"
    }

    private fun mediaPreview(message: ChatMessage): String? {
        if (message.extra.isBlank()) return null
        return runCatching { MediaPayload.parse(message.extra).preview() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    private val WHITESPACE = Regex("\\s+")
}
