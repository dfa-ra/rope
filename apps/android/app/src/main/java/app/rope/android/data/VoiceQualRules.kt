package app.rope.android.data

/**
 * Settings voice-note send quality. Default is today's AAC 64 kbps / 44.1 kHz.
 * Stored in kv (`voice_qual`). LocalStore stays v6.
 */
object VoiceQualRules {
    const val COMPRESSED = ""
    const val HD = "hd"
    const val TITLE = "Качество голоса"
    const val KEY = "voice_qual"
    const val BITRATE_COMPRESSED = 64_000
    const val BITRATE_HD = 96_000
    const val SAMPLE_COMPRESSED = 44_100
    const val SAMPLE_HD = 48_000

    data class Option(val id: String, val label: String)

    val OPTIONS: List<Option> = listOf(
        Option(COMPRESSED, "сжатие"),
        Option(HD, "HD"),
    )

    fun normalize(raw: String?): String {
        val v = raw?.trim().orEmpty()
        if (v.isEmpty()) return COMPRESSED
        return OPTIONS.find { it.id == v }?.id ?: COMPRESSED
    }

    fun bitrate(id: String): Int = when (normalize(id)) {
        HD -> BITRATE_HD
        else -> BITRATE_COMPRESSED
    }

    fun sampleRate(id: String): Int = when (normalize(id)) {
        HD -> SAMPLE_HD
        else -> SAMPLE_COMPRESSED
    }

    fun hint(): String =
        "Сжатие как сейчас, AAC 64 кбит/с. HD — 96 кбит/с. Только голосовые, не звонок. Не FCM."
}
