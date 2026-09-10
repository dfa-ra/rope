package app.rope.android.data

/**
 * Telegram-like jump to the first (oldest) local message in the open
 * thread. Reuses scrollToMessageId. No LocalStore bump.
 */
object FirstMessageRules {
    const val ACTION = "В начало"

    fun id(messages: List<ChatMessage>): String? =
        messages
            .filter { !it.deleted }
            .minWithOrNull(compareBy({ it.timestampMs }, { it.id }))
            ?.id

    fun visible(id: String?): Boolean = !id.isNullOrBlank()
}
