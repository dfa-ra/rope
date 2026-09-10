package app.rope.android.data

/**
 * Telegram-like tap on «изм.»: show the text that [LocalStore.editMessage]
 * overwrote. [ChatMessage.edited] stays a boolean; history is one local prior
 * (the original), stored in message meta. Not envelope crypto.
 */
object EditHistoryRules {
    const val LABEL = "изм."
    const val TITLE = "Было"
    const val CLOSE = "Закрыть"
    const val MISSING = "Предыдущий текст недоступен"

    fun showLabel(edited: Boolean, deleted: Boolean = false): Boolean = edited && !deleted

    fun clickable(edited: Boolean, prior: String?, deleted: Boolean = false): Boolean =
        showLabel(edited, deleted) && !JsonIds.optional(prior).isNullOrBlank()

    /** Keep the original body across re-edits. */
    fun capture(existingPrior: String?, oldText: String): String {
        val kept = JsonIds.optional(existingPrior)
        if (kept != null) return kept
        return oldText
    }

    fun persist(edited: Boolean, prior: String?): String? =
        if (edited) JsonIds.optional(prior) else null

    fun dialogBody(prior: String?): String = JsonIds.optional(prior) ?: MISSING
}
