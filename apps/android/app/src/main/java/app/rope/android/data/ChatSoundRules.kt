package app.rope.android.data

/**
 * Per-chat notification sound. Stored on ChatPrefs JSON (`sound`), so LocalStore
 * stays v6. Not call tones, not FCM, not envelope crypto.
 */
object ChatSoundRules {
    const val DEFAULT = ""
    const val NONE = "none"
    const val NOTE = "note"
    const val PING = "ping"
    const val POP = "pop"
    const val TITLE = "Звук уведомлений"
    const val DURATION_MS = 160
    const val VOLUME = 80

    /** ToneGenerator.TONE_PROP_ACK / BEEP / NACK. 0 = channel default, -1 = silent. */
    const val TONE_CHANNEL = 0
    const val TONE_SILENT = -1
    const val TONE_NOTE = 25
    const val TONE_PING = 27
    const val TONE_POP = 26

    data class Option(val id: String, val label: String, val tone: Int)

    val OPTIONS: List<Option> = listOf(
        Option(DEFAULT, "по умолчанию", TONE_CHANNEL),
        Option(NONE, "без звука", TONE_SILENT),
        Option(NOTE, "нота", TONE_NOTE),
        Option(PING, "пинг", TONE_PING),
        Option(POP, "щелчок", TONE_POP),
    )

    fun normalize(raw: String?): String {
        val v = raw?.trim().orEmpty()
        if (v.isEmpty()) return DEFAULT
        return OPTIONS.find { it.id == v }?.id ?: DEFAULT
    }

    fun label(id: String): String =
        OPTIONS.find { it.id == normalize(id) }?.label ?: OPTIONS.first().label

    fun tone(id: String): Int =
        OPTIONS.find { it.id == normalize(id) }?.tone ?: TONE_CHANNEL

    fun isSilent(id: String): Boolean = normalize(id) == NONE

    fun playsTone(id: String): Boolean = tone(id) > 0

    /** Custom tone or mute should not also play the channel default. */
    fun suppressChannel(id: String): Boolean = isSilent(id) || playsTone(id)

    fun showsPicker(saved: Boolean): Boolean = !saved
}
