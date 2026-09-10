package app.rope.android.data

/**
 * Settings importance for the message shade channel (heads-up vs default).
 * Not channel sound, not vibration, not in-app beep. LocalStore kv; not envelope crypto.
 */
object NotifyPrioRules {
    const val DEFAULT = "default"
    const val HIGH = "high"
    const val KEY = "notify_prio"
    const val TITLE = "Всплывающие"
    const val CHANNEL_DEFAULT = "rope-messages"

    /** NotificationManager.IMPORTANCE_DEFAULT */
    const val IMPORTANCE_DEFAULT = 3

    /** NotificationManager.IMPORTANCE_HIGH */
    const val IMPORTANCE_HIGH = 4

    /** NotificationCompat.PRIORITY_DEFAULT */
    const val PRIORITY_DEFAULT = 0

    /** NotificationCompat.PRIORITY_HIGH */
    const val PRIORITY_HIGH = 1

    data class Option(val id: String, val label: String)

    val OPTIONS: List<Option> = listOf(
        Option(DEFAULT, "Обычные"),
        Option(HIGH, "Всплывающие"),
    )

    fun sanitize(raw: String?): String? {
        if (raw.isNullOrBlank()) return DEFAULT
        if ('\n' in raw || '\r' in raw || '\u0000' in raw) return null
        val v = raw.trim()
        return OPTIONS.find { it.id == v }?.id
    }

    fun normalize(raw: String?): String = sanitize(raw) ?: DEFAULT

    fun isHigh(raw: String?): Boolean = normalize(raw) == HIGH

    fun channelId(raw: String?): String {
        val id = normalize(raw)
        return if (id == DEFAULT) CHANNEL_DEFAULT else "$CHANNEL_DEFAULT-$id"
    }

    fun importance(raw: String?): Int =
        if (isHigh(raw)) IMPORTANCE_HIGH else IMPORTANCE_DEFAULT

    fun compatPriority(raw: String?): Int =
        if (isHigh(raw)) PRIORITY_HIGH else PRIORITY_DEFAULT

    fun label(raw: String?): String =
        OPTIONS.find { it.id == normalize(raw) }?.label ?: OPTIONS.first().label

    fun hint(): String =
        "Баннер поверх экрана. Не тон шторки и не вибрация."
}
