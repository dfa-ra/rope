package app.rope.android.data

/**
 * Telegram-like composer cap for chat text: 4096 Unicode runes (code points),
 * not UTF-16 units, so a supplementary-plane emoji counts as one. Media
 * captions stay [MediaSendRules.CAPTION_MAX] (caption-count is a separate
 * ticket). LocalStore stays v6.
 */
object TextLimitRules {
    const val TEXT_MAX = 4096
    const val WARN_REMAINING = 200

    fun runeCount(raw: String): Int =
        if (raw.isEmpty()) 0 else raw.codePointCount(0, raw.length)

    fun takeRunes(raw: String, max: Int): String {
        if (max <= 0 || raw.isEmpty()) return ""
        val n = runeCount(raw)
        if (n <= max) return raw
        return raw.substring(0, raw.offsetByCodePoints(0, max))
    }

    fun limit(raw: String): String = takeRunes(raw, TEXT_MAX)

    fun countedLength(raw: String): Int = runeCount(raw).coerceIn(0, TEXT_MAX)

    fun counter(raw: String): String = "${countedLength(raw)} / $TEXT_MAX"

    fun atLimit(raw: String): Boolean = runeCount(raw) >= TEXT_MAX

    fun showCounter(raw: String, pendingMedia: Boolean): Boolean {
        if (pendingMedia) return false
        return runeCount(raw) >= TEXT_MAX - WARN_REMAINING
    }

    fun forComposer(raw: String, pendingMedia: Boolean): String {
        if (pendingMedia) {
            return if (raw.length <= MediaSendRules.CAPTION_MAX) raw else raw.take(MediaSendRules.CAPTION_MAX)
        }
        return limit(raw)
    }
}
