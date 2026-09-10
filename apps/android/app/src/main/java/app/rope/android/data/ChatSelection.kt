package app.rope.android.data

/**
 * Telegram-like multi-select: «Выбрать» from the bubble menu, then copy
 * one or many texts. No protocol — clipboard only.
 */
object ChatSelection {
    const val MENU = "Выбрать"
    const val COPY = "Копировать"

    fun title(count: Int): String = "Выбрано $count"

    fun canSelect(msg: ChatMessage): Boolean = !msg.deleted

    /**
     * Clipboard payload. A single text is copied as-is; several rows get
     * «Вы:» / sender prefixes like Telegram's multi-copy.
     */
    fun copyText(msgs: List<ChatMessage>, you: String = GroupChatUx.YOU): String {
        val ok = msgs.filter { ChatActions.canCopy(it) }
        if (ok.isEmpty()) return ""
        if (ok.size == 1) return ok.first().text
        return ok.joinToString("\n\n") { m ->
            val name = when {
                m.outgoing -> you
                m.senderName.isNotBlank() -> m.senderName.trim()
                else -> "сообщение"
            }
            "$name: ${m.text.trim()}"
        }
    }
}
