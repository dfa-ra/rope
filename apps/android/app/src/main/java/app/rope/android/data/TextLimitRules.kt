package app.rope.android.data

/**
 * Telegram-like composer cap for chat text (not media captions — those stay
 * [MediaSendRules.CAPTION_MAX]). UTF-16 length, same as Kotlin [String.length].
 */
object TextLimitRules {
    const val TEXT_MAX = 4096
    const val WARN_REMAINING = 200

    fun limit(raw: String): String =
        if (raw.length <= TEXT_MAX) raw else raw.take(TEXT_MAX)

    fun countedLength(raw: String): Int = raw.length.coerceIn(0, TEXT_MAX)

    fun counter(raw: String): String = "${countedLength(raw)} / $TEXT_MAX"

    fun atLimit(raw: String): Boolean = countedLength(raw) >= TEXT_MAX

    fun showCounter(raw: String, pendingMedia: Boolean): Boolean {
        if (pendingMedia) return false
        return raw.length >= TEXT_MAX - WARN_REMAINING
    }

    fun forComposer(raw: String, pendingMedia: Boolean): String {
        if (pendingMedia) {
            return if (raw.length <= MediaSendRules.CAPTION_MAX) raw else raw.take(MediaSendRules.CAPTION_MAX)
        }
        return limit(raw)
    }
}
