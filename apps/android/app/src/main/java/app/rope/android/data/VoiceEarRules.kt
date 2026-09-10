package app.rope.android.data

/**
 * Telegram-like earpiece routing for voice notes. Default is the loudspeaker
 * (USAGE_MEDIA). On uses USAGE_VOICE_COMMUNICATION. Live calls keep their own
 * route. Kv-only. LocalStore stays v6.
 */
object VoiceEarRules {
    const val TITLE = "Наушник для голосовых"
    const val SECTION = "Чат"
    const val KV = "voice_ear"

    fun earpieceFromKv(raw: String?): Boolean = raw == "1"

    fun useEarpiece(enabled: Boolean, liveCall: Boolean): Boolean =
        enabled && !liveCall

    fun hint(): String =
        "Как у трубки. Выкл — голосовые идут в динамик."
}
