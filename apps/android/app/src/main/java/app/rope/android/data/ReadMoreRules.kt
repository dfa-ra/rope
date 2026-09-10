package app.rope.android.data

/**
 * Telegram-like «Ещё» on long text bubbles. Expand stays expanded.
 * Does not bump LocalStore. Not FCM.
 */
object ReadMoreRules {
    const val PREVIEW_CHARS = 400
    const val PREVIEW_LINES = 8
    const val LABEL = "Ещё"

    fun lineCount(text: String): Int {
        if (text.isEmpty()) return 0
        return text.count { it == '\n' } + 1
    }

    fun needsCollapse(text: String): Boolean {
        if (text.length > PREVIEW_CHARS) return true
        return lineCount(text) > PREVIEW_LINES
    }

    fun shown(text: String, expanded: Boolean): String =
        if (expanded || !needsCollapse(text)) text else collapsed(text)

    fun collapsed(text: String): String {
        if (!needsCollapse(text)) return text
        val byLines = clipLines(text)
        val byChars = clipChars(byLines)
        return if (byChars == text) text else byChars.trimEnd() + "…"
    }

    fun showLabel(text: String, expanded: Boolean): Boolean =
        needsCollapse(text) && !expanded

    private fun clipLines(text: String): String {
        if (lineCount(text) <= PREVIEW_LINES) return text
        var seen = 0
        val cut = text.indexOfFirst { ch ->
            if (ch == '\n') {
                seen++
                seen == PREVIEW_LINES
            } else {
                false
            }
        }
        return if (cut < 0) text else text.substring(0, cut)
    }

    private fun clipChars(text: String): String {
        if (text.length <= PREVIEW_CHARS) return text
        val hard = text.substring(0, PREVIEW_CHARS)
        val ws = hard.indexOfLast { it.isWhitespace() }
        return if (ws >= PREVIEW_CHARS / 2) hard.substring(0, ws) else hard
    }
}
