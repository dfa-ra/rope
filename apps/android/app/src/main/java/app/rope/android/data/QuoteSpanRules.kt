package app.rope.android.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Telegram-like partial quote (Replies 2.0). Offsets are UTF-16 code units,
 * matching [String] indices. Packed as inner JSON `qt` / `qo` and encrypted
 * by the existing Rust path — Kotlin never implements crypto.
 */
data class QuoteSpan(val start: Int, val end: Int) {
    val length: Int get() = (end - start).coerceAtLeast(0)
}

data class PackedQuote(
    val text: String,
    val start: Int,
    val end: Int,
) {
    fun span(): QuoteSpan = QuoteSpan(start, end)
}

object QuoteSpanRules {
    fun canSelect(text: String): Boolean = text.isNotBlank()

    fun canSelect(msg: ChatMessage): Boolean =
        !msg.deleted &&
            (msg.kind == MessageKind.TEXT || msg.kind == MessageKind.GROUP_TEXT) &&
            canSelect(msg.text)

    fun snap(text: String, index: Int, towardStart: Boolean): Int {
        var i = index.coerceIn(0, text.length)
        if (i in 1 until text.length && text[i].isLowSurrogate() && text[i - 1].isHighSurrogate()) {
            i = if (towardStart) i - 1 else (i + 1).coerceAtMost(text.length)
        }
        return i
    }

    fun clamp(source: String, start: Int, end: Int): QuoteSpan? {
        if (source.isEmpty()) return null
        val s = snap(source, start, towardStart = true)
        val e = snap(source, end, towardStart = false)
        if (s >= e) return null
        return QuoteSpan(s, e)
    }

    fun excerpt(source: String, span: QuoteSpan): String = source.substring(span.start, span.end)

    fun isPartial(source: String, span: QuoteSpan): Boolean =
        span.start > 0 || span.end < source.length

    /**
     * A sendable span: valid substring of [source] that is not the whole body.
     * Full-body replies omit `qt`/`qo` and keep today's `rp` preview.
     */
    fun packed(source: String, span: QuoteSpan?): PackedQuote? {
        val clamped = span?.let { clamp(source, it.start, it.end) } ?: return null
        if (!isPartial(source, clamped)) return null
        val text = excerpt(source, clamped)
        if (text.isBlank()) return null
        return PackedQuote(text, clamped.start, clamped.end)
    }

    fun preview(source: String, span: QuoteSpan?): String = packed(source, span)?.text ?: source

    fun displayPreview(replyPreview: String, quoteText: String): String =
        quoteText.trim().ifBlank { replyPreview }

    fun parseOffsets(raw: JSONArray?): QuoteSpan? {
        if (raw == null || raw.length() < 2) return null
        val start = raw.optInt(0, -1)
        val end = raw.optInt(1, -1)
        if (start < 0 || end <= start) return null
        return QuoteSpan(start, end)
    }

    fun put(o: JSONObject, packed: PackedQuote?) {
        if (packed == null || packed.text.isBlank()) return
        o.put("qt", packed.text)
        o.put("qo", JSONArray().put(packed.start).put(packed.end))
    }

    fun put(o: JSONObject, quoteText: String, quoteStart: Int, quoteEnd: Int) {
        if (quoteText.isBlank() || quoteStart < 0 || quoteEnd <= quoteStart) return
        o.put("qt", quoteText)
        o.put("qo", JSONArray().put(quoteStart).put(quoteEnd))
    }

    fun read(o: JSONObject): PackedQuote? {
        val qt = o.optString("qt")
        val span = parseOffsets(o.optJSONArray("qo"))
        if (qt.isBlank() && span == null) return null
        return PackedQuote(qt, span?.start ?: -1, span?.end ?: -1)
    }
}
