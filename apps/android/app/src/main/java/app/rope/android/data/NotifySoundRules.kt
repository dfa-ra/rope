package app.rope.android.data

/**
 * Settings tone for the message notification channel (shade / lockscreen).
 * Not in-app heads-up beep, not per-chat sound, not call ringtone.
 * LocalStore kv; not envelope crypto.
 */
object NotifySoundRules {
    const val DEFAULT = "default"
    const val NONE = "none"
    const val RING = "ring"
    const val KEY = "notify_sound"
    const val TITLE = "Звук уведомлений"
    const val CHANNEL_DEFAULT = "rope-messages"

    data class Option(val id: String, val label: String)

    val OPTIONS: List<Option> = listOf(
        Option(DEFAULT, "Стандарт"),
        Option(RING, "Рингтон"),
        Option(NONE, "Нет"),
    )

    fun sanitize(raw: String?): String? {
        if (raw.isNullOrBlank()) return DEFAULT
        if ('\n' in raw || '\r' in raw || '\u0000' in raw) return null
        val v = raw.trim()
        return OPTIONS.find { it.id == v }?.id
    }

    fun normalize(raw: String?): String = sanitize(raw) ?: DEFAULT

    fun isSilent(raw: String?): Boolean = normalize(raw) == NONE

    fun useRingtone(raw: String?): Boolean = normalize(raw) == RING

    fun channelId(raw: String?): String {
        val id = normalize(raw)
        return if (id == DEFAULT) CHANNEL_DEFAULT else "$CHANNEL_DEFAULT-$id"
    }

    fun label(raw: String?): String =
        OPTIONS.find { it.id == normalize(raw) }?.label ?: OPTIONS.first().label

    fun hint(): String =
        "Тон в шторке. Не звук открытого чата и не рингтон звонка."
}
