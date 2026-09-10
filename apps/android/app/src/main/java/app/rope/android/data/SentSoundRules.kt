package app.rope.android.data

/**
 * Telegram-like in-app sound when you send a message. Default on.
 * Global "Без звука" still wins. Not call tones, not FCM.
 */
object SentSoundRules {
    const val TITLE = "Звук отправки"
    const val KV = "sent_sound"
    /** ToneGenerator.TONE_PROP_ACK */
    const val TONE = 25
    const val DURATION_MS = 90
    const val VOLUME = 70

    fun enabledFromKv(raw: String?): Boolean = raw != "0"

    fun shouldPlay(enabled: Boolean, globalMuted: Boolean): Boolean =
        enabled && !globalMuted

    fun hint(): String =
        "Короткий звук, когда уходит сообщение. Не звонок."
}
