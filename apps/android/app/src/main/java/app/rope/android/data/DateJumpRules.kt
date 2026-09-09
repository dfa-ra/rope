package app.rope.android.data

import java.util.TimeZone

data class DateJumpChoice(
    val dayKey: String,
    val label: String,
    val messageId: String,
)

/**
 * Telegram-like jump-to-date in a thread. Days come from local
 * [DateSeparatorRules] chips. LocalStore schema stays v6.
 */
object DateJumpRules {
    const val ACTION = "К дате"
    const val TITLE = "Перейти к дате"
    const val EMPTY = "Нет сообщений"
    const val CLOSE = "Закрыть"

    fun showButton(messages: List<ChatMessage>): Boolean =
        messages.any { !it.deleted }

    fun choices(
        messages: List<ChatMessage>,
        nowMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault(),
    ): List<DateJumpChoice> {
        val live = messages.filter { !it.deleted }
        if (live.isEmpty()) return emptyList()
        return DateSeparatorRules.grouped(live, nowMs, timeZone)
            .map { g ->
                DateJumpChoice(
                    dayKey = g.dayKey,
                    label = g.label,
                    messageId = g.messages.first().id,
                )
            }
            .asReversed()
    }

    fun firstId(
        messages: List<ChatMessage>,
        dayKey: String,
        nowMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault(),
    ): String? = choices(messages, nowMs, timeZone).firstOrNull { it.dayKey == dayKey }?.messageId
}
