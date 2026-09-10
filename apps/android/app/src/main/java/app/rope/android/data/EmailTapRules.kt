package app.rope.android.data

/**
 * Tappable emails in message text. Opens the system mail composer.
 * Reject CR/LF/NUL in the matched slice before trim. Not FCM.
 */
data class EmailSpan(
    val start: Int,
    val endExclusive: Int,
    val mailto: String,
)

object EmailTapRules {
    const val LOCAL_MAX = 64
    const val ADDR_MAX = 254

    private val finder = Regex(
        """(?<![A-Za-z0-9._%+-])([A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,24})(?![A-Za-z0-9._%+-])""",
    )

    fun spans(text: String, occupied: List<IntRange> = emptyList()): List<EmailSpan> {
        if (text.isEmpty()) return emptyList()
        val out = mutableListOf<EmailSpan>()
        finder.findAll(text).forEach { match ->
            val raw = match.value
            if (hasCtl(raw)) return@forEach
            val mailto = mailtoUri(raw) ?: return@forEach
            val start = match.range.first
            val end = start + raw.length
            if (end <= start || end > text.length) return@forEach
            val range = start until end
            if (occupied.any { overlaps(it, range) }) return@forEach
            out += EmailSpan(start, end, mailto)
        }
        return out
    }

    fun mailtoUri(raw: String): String? {
        if (hasCtl(raw)) return null
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed.length > ADDR_MAX) return null
        val at = trimmed.indexOf('@')
        if (at <= 0 || at != trimmed.lastIndexOf('@')) return null
        val local = trimmed.substring(0, at)
        val domain = trimmed.substring(at + 1)
        if (local.length > LOCAL_MAX) return null
        if (local.startsWith('.') || local.endsWith('.') || ".." in local) return null
        if (domain.startsWith('.') || domain.endsWith('.') || ".." in domain) return null
        val labels = domain.split('.')
        if (labels.size < 2) return null
        if (labels.any { it.isEmpty() || it.startsWith('-') || it.endsWith('-') }) return null
        if (!local.all { it.isLetterOrDigit() || it in "._%+-" }) return null
        if (!domain.all { it.isLetterOrDigit() || it == '.' || it == '-' }) return null
        val tld = labels.last()
        if (tld.length < 2 || !tld.all { it.isLetter() }) return null
        return "mailto:$trimmed"
    }

    private fun hasCtl(s: String): Boolean =
        s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0 || s.indexOf('\u0000') >= 0

    private fun overlaps(a: IntRange, b: IntRange): Boolean =
        a.first <= b.last && b.first <= a.last
}
