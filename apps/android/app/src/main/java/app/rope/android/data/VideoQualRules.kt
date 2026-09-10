package app.rope.android.data

/**
 * Settings video send quality. Default is today's 720p ladder.
 * Original skips recode when the file fits the 25 MiB cap. Stored in kv
 * (`video_qual`). LocalStore stays v6.
 */
object VideoQualRules {
    const val COMPRESSED = ""
    const val HD = "hd"
    const val ORIGINAL = "original"
    const val TITLE = "Качество видео"
    const val KEY = "video_qual"
    const val HEIGHT_COMPRESSED = VideoRules.TARGET_HEIGHT
    const val HEIGHT_HD = 1080

    data class Option(val id: String, val label: String)

    val OPTIONS: List<Option> = listOf(
        Option(COMPRESSED, "сжатие"),
        Option(HD, "HD"),
        Option(ORIGINAL, "оригинал"),
    )

    fun normalize(raw: String?): String {
        val v = raw?.trim().orEmpty()
        if (v.isEmpty()) return COMPRESSED
        return OPTIONS.find { it.id == v }?.id ?: COMPRESSED
    }

    fun skipTranscode(id: String, size: Long, mime: String, name: String): Boolean {
        val n = normalize(id)
        if (n == ORIGINAL) return VideoRules.fitsCap(size)
        return !VideoRules.mustCompress(size, mime, name)
    }

    fun heights(id: String): List<Int> = when (normalize(id)) {
        HD -> listOf(HEIGHT_HD, HEIGHT_COMPRESSED, VideoRules.FALLBACK_HEIGHT)
        else -> listOf(HEIGHT_COMPRESSED, VideoRules.FALLBACK_HEIGHT, 360)
    }

    fun keepOriginalContainer(id: String): Boolean = normalize(id) == ORIGINAL

    fun hint(): String =
        "Сжатие 720p как сейчас. HD — 1080p. Оригинал без перекодирования, если влезает в 25 МиБ. Не FCM."
}
