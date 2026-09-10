package app.rope.android.data

/**
 * Settings vibration for the message notification channel (shade).
 * Not in-app heads-up pulse, not per-chat vibe, not call vibration.
 * LocalStore kv; not envelope crypto.
 */
object NotifyVibRules {
    const val KEY = "notify_vibrate"
    const val TITLE = "Вибрация"
    const val CHANNEL_DEFAULT = "rope-messages"
    const val CHANNEL_OFF = "rope-messages-novib"

    fun enabledFromKv(raw: String?): Boolean = raw != "0"

    fun channelId(enabled: Boolean): String =
        if (enabled) CHANNEL_DEFAULT else CHANNEL_OFF

    fun hint(): String =
        "Вибрация шторки у входящего. Не звонок и не открытый чат."
}
