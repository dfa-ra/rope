package app.rope.android.data

/**
 * Telegram-like *bold* / _italic_ / `code` / ```pre``` / ~strike~ inside the
 * existing ciphertext `t` field. TextBody still packs t / reply / quote / lp
 * only — no entity JSON, no FCM.
 */
enum class TextFmtKind { BOLD, ITALIC, CODE, PRE, STRIKE }

data class TextFmtSpan(
    val start: Int,
    val endExclusive: Int,
    val kind: TextFmtKind,
)

data class FormattedText(
    val display: String,
    val spans: List<TextFmtSpan>,
)

object TextFmtRules {
    private val lang = Regex("^[A-Za-z0-9_+-]*$")

    fun plain(raw: String): String = parse(raw).display

    fun parse(raw: String): FormattedText {
        if (raw.isEmpty()) return FormattedText("", emptyList())
        val out = StringBuilder()
        val spans = mutableListOf<TextFmtSpan>()
        consume(raw, 0, raw.length, out, spans, emptySet())
        return FormattedText(out.toString(), spans)
    }

    private fun consume(
        raw: String,
        from: Int,
        to: Int,
        out: StringBuilder,
        spans: MutableList<TextFmtSpan>,
        open: Set<TextFmtKind>,
    ) {
        var i = from
        while (i < to) {
            if (raw[i] == '\\' && i + 1 < to && isEscapable(raw[i + 1])) {
                out.append(raw[i + 1])
                i += 2
                continue
            }
            val next = takeDelimited(raw, i, to, out, spans, open)
            if (next != null) {
                i = next
                continue
            }
            out.append(raw[i])
            i++
        }
    }

    private fun isEscapable(c: Char): Boolean =
        c == '*' || c == '_' || c == '`' || c == '~' || c == '\\'

    private fun takeDelimited(
        raw: String,
        i: Int,
        to: Int,
        out: StringBuilder,
        spans: MutableList<TextFmtSpan>,
        open: Set<TextFmtKind>,
    ): Int? {
        if (raw.startsWith("```", i) && TextFmtKind.PRE !in open) {
            val close = closer(raw, i + 3, to, "```", multiline = true) ?: return null
            val inner = preInner(raw, i + 3, close) ?: return null
            emitAtomic(out, spans, inner, TextFmtKind.PRE)
            return close + 3
        }
        if (raw.startsWith("`", i) && TextFmtKind.CODE !in open && TextFmtKind.PRE !in open) {
            val close = closer(raw, i + 1, to, "`", multiline = false) ?: return null
            emitAtomic(out, spans, raw.substring(i + 1, close), TextFmtKind.CODE)
            return close + 1
        }
        if (raw.startsWith("*", i) && TextFmtKind.BOLD !in open) {
            val close = closer(raw, i + 1, to, "*", multiline = false) ?: return null
            emitNested(raw, i + 1, close, out, spans, open, TextFmtKind.BOLD)
            return close + 1
        }
        if (raw.startsWith("_", i) && TextFmtKind.ITALIC !in open) {
            val close = closer(raw, i + 1, to, "_", multiline = false) ?: return null
            emitNested(raw, i + 1, close, out, spans, open, TextFmtKind.ITALIC)
            return close + 1
        }
        if (raw.startsWith("~~", i) && TextFmtKind.STRIKE !in open) {
            val close = closer(raw, i + 2, to, "~~", multiline = false) ?: return null
            emitNested(raw, i + 2, close, out, spans, open, TextFmtKind.STRIKE)
            return close + 2
        }
        if (raw.startsWith("~", i) && TextFmtKind.STRIKE !in open) {
            val close = closer(raw, i + 1, to, "~", multiline = false) ?: return null
            emitNested(raw, i + 1, close, out, spans, open, TextFmtKind.STRIKE)
            return close + 1
        }
        return null
    }

    private fun emitAtomic(
        out: StringBuilder,
        spans: MutableList<TextFmtSpan>,
        inner: String,
        kind: TextFmtKind,
    ) {
        val start = out.length
        out.append(inner)
        spans += TextFmtSpan(start, out.length, kind)
    }

    private fun emitNested(
        raw: String,
        contentFrom: Int,
        contentTo: Int,
        out: StringBuilder,
        spans: MutableList<TextFmtSpan>,
        open: Set<TextFmtKind>,
        kind: TextFmtKind,
    ) {
        val start = out.length
        consume(raw, contentFrom, contentTo, out, spans, open + kind)
        spans += TextFmtSpan(start, out.length, kind)
    }

    private fun closer(raw: String, from: Int, to: Int, marker: String, multiline: Boolean): Int? {
        var i = from
        while (i + marker.length <= to) {
            if (!multiline && raw[i] == '\n') return null
            if (raw[i] == '\\' && i + 1 < to && isEscapable(raw[i + 1])) {
                i += 2
                continue
            }
            if (i > from && raw.startsWith(marker, i)) return i
            i++
        }
        return null
    }

    private fun preInner(raw: String, contentStart: Int, close: Int): String? {
        if (close <= contentStart) return null
        var s = contentStart
        val nl = raw.indexOf('\n', s)
        if (nl in s until close) {
            val head = raw.substring(s, nl)
            if (lang.matches(head)) s = nl + 1
        }
        var e = close
        if (e > s && raw[e - 1] == '\n') e--
        if (e <= s) return null
        return raw.substring(s, e)
    }
}
