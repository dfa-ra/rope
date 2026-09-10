package app.rope.android.data

data class HashtagSpan(
    val start: Int,
    val endExclusive: Int,
    val tag: String,
)

/**
 * Telegram-like tap on `#tag` fills in-chat search. Not a URL, not an
 * @mention. LocalStore stays v6.
 */
object HashtagRules {
    const val TAG_MAX = 64

    fun spans(text: String): List<HashtagSpan> {
        if (text.isEmpty()) return emptyList()
        val links = LinkPreviewRules.spans(text)
        val out = mutableListOf<HashtagSpan>()
        finder.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            if (end <= start || end > text.length) return@forEach
            if (links.any { it.start < end && start < it.endExclusive }) return@forEach
            val tag = text.substring(start, end)
            if (tag.any { it == '\n' || it == '\r' || it == '\u0000' }) return@forEach
            out += HashtagSpan(start, end, tag)
        }
        return out
    }

    fun query(raw: String?): String? {
        val cut = raw.orEmpty()
        if (cut.any { it == '\n' || it == '\r' || it == '\u0000' }) return null
        val tagged = if (cut.startsWith("#")) cut else "#$cut"
        if (tagged.length > TAG_MAX + 1) return null
        val one = spans(tagged).singleOrNull() ?: return null
        if (one.start != 0 || one.endExclusive != tagged.length) return null
        return one.tag
    }

    private val finder = Regex("(?<![\\p{L}\\p{N}_])#([\\p{L}\\p{N}_]{1,$TAG_MAX})")
}
