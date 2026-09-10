package app.rope.android.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class MessageInfoRow(
    val label: String,
    val value: String,
)

/**
 * Telegram-like message info sheet. Local timestamps + envelope status only —
 * no read receipts protocol, no Go, no LocalStore bump.
 */
object MessageInfoRules {
    const val LABEL = "Информация"
    const val SENT = "Отправлено"
    const val STATUS = "Статус"
    const val EDITED = "Изменено"
    const val EDITED_YES = "да"
    const val WAITING = "Ожидает отправки"
    const val SENT_SERVER = "Отправлено"
    const val DELIVERED = "Доставлено"

    fun canShow(msg: ChatMessage): Boolean = !msg.deleted

    fun statusCopy(msg: ChatMessage): String? {
        if (!msg.outgoing) return null
        return when (msg.status) {
            MessageStatus.CREATED -> WAITING
            MessageStatus.SENT_TO_SERVER -> SENT_SERVER
            MessageStatus.DELIVERED_TO_DEVICE -> DELIVERED
        }
    }

    fun sentAt(
        timestampMs: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = DateSeparatorRules.RU,
    ): String {
        val fmt = SimpleDateFormat("d MMMM yyyy, HH:mm", locale)
        fmt.timeZone = timeZone
        return fmt.format(Date(timestampMs))
    }

    fun rows(
        msg: ChatMessage,
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = DateSeparatorRules.RU,
    ): List<MessageInfoRow> {
        if (!canShow(msg)) return emptyList()
        val out = mutableListOf(MessageInfoRow(SENT, sentAt(msg.timestampMs, timeZone, locale)))
        statusCopy(msg)?.let { out += MessageInfoRow(STATUS, it) }
        if (msg.edited) out += MessageInfoRow(EDITED, EDITED_YES)
        return out
    }
}
