package app.rope.android.data

/**
 * Tappable phone numbers in message text. Opens the system dialer.
 * Reject CR/LF/NUL in the matched slice before trim. Not FCM.
 */
data class PhoneSpan(
    val start: Int,
    val endExclusive: Int,
    val tel: String,
)

object PhoneTapRules {
    const val MIN_DIGITS = 7
    const val MAX_DIGITS = 15

    private val finder = Regex("""(?<!\d)(\+?\d[\d\s\-()]{5,24}\d)(?!\d)""")

    fun spans(text: String, occupied: List<IntRange> = emptyList()): List<PhoneSpan> {
        if (text.isEmpty()) return emptyList()
        val out = mutableListOf<PhoneSpan>()
        finder.findAll(text).forEach { match ->
            val raw = match.value
            if (hasCtl(raw)) return@forEach
            val tel = telUri(raw) ?: return@forEach
            val start = match.range.first
            val end = start + raw.length
            if (end <= start || end > text.length) return@forEach
            val range = start until end
            if (occupied.any { overlaps(it, range) }) return@forEach
            out += PhoneSpan(start, end, tel)
        }
        return out
    }

    fun telUri(raw: String): String? {
        if (hasCtl(raw)) return null
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val compact = buildString {
            trimmed.forEach { ch ->
                when {
                    ch.isDigit() -> append(ch)
                    ch == '+' && isEmpty() -> append(ch)
                }
            }
        }
        val digits = compact.filter { it.isDigit() }
        if (digits.length < MIN_DIGITS || digits.length > MAX_DIGITS) return null
        if (compact.indexOf('\n') >= 0) return null
        return "tel:$compact"
    }

    private fun hasCtl(s: String): Boolean =
        s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0 || s.indexOf('\u0000') >= 0

    private fun overlaps(a: IntRange, b: IntRange): Boolean =
        a.first <= b.last && b.first <= a.last
}
