package app.rope.android.data

/**
 * Settings photo send quality. Default is today's JPEG 2048px / q85.
 * Original skips recode. Stored in kv (`photo_qual`). LocalStore stays v6.
 */
object PhotoQualRules {
    const val COMPRESSED = ""
    const val HD = "hd"
    const val ORIGINAL = "original"
    const val TITLE = "Качество фото"
    const val KEY = "photo_qual"
    const val EDGE_COMPRESSED = 2048
    const val EDGE_HD = 2560
    const val JPEG_COMPRESSED = 85
    const val JPEG_HD = 90

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

    fun compress(id: String): Boolean = normalize(id) != ORIGINAL

    fun maxEdge(id: String): Int = when (normalize(id)) {
        HD -> EDGE_HD
        else -> EDGE_COMPRESSED
    }

    fun jpegQuality(id: String): Int = when (normalize(id)) {
        HD -> JPEG_HD
        else -> JPEG_COMPRESSED
    }

    fun hint(): String =
        "Сжатие как сейчас. HD — крупнее JPEG. Оригинал без перекодирования. Не FCM."
}
