package app.rope.android.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Telegram spoilers as inner JSON `sp` ranges (UTF-16). Blur until tap.
 * Not envelope crypto — Rust still encrypts the whole body.
 */
object SpoilerRules {
    const val MARK = "||"

    fun parse(raw: String): Pair<String, List<IntRange>> {
        if (!raw.contains(MARK)) return raw to emptyList()
        val out = StringBuilder()
        val ranges = mutableListOf<IntRange>()
        var i = 0
        while (i < raw.length) {
            val open = raw.indexOf(MARK, i)
            if (open < 0) {
                out.append(raw, i, raw.length)
                break
            }
            out.append(raw, i, open)
            val close = raw.indexOf(MARK, open + MARK.length)
            if (close < 0) {
                out.append(raw, open, raw.length)
                break
            }
            val inner = raw.substring(open + MARK.length, close)
            val start = out.length
            out.append(inner)
            if (inner.isNotEmpty()) ranges += start until out.length
            i = close + MARK.length
        }
        return out.toString() to ranges
    }

    fun wrap(plain: String, ranges: List<IntRange>): String {
        if (ranges.isEmpty()) return plain
        val sb = StringBuilder(plain)
        ranges.sortedByDescending { it.first }.forEach { r ->
            val end = r.last + 1
            if (r.first < 0 || end > sb.length || r.first >= end) return@forEach
            sb.insert(end, MARK)
            sb.insert(r.first, MARK)
        }
        return sb.toString()
    }

    fun put(o: JSONObject, ranges: List<IntRange>) {
        if (ranges.isEmpty()) return
        val arr = JSONArray()
        ranges.forEach { r ->
            if (r.first < r.last + 1) arr.put(JSONArray().put(r.first).put(r.last + 1))
        }
        if (arr.length() > 0) o.put("sp", arr)
    }

    fun read(o: JSONObject): List<IntRange> {
        val arr = o.optJSONArray("sp") ?: return emptyList()
        val out = mutableListOf<IntRange>()
        for (i in 0 until arr.length()) {
            val pair = arr.optJSONArray(i) ?: continue
            if (pair.length() < 2) continue
            val start = pair.optInt(0, -1)
            val end = pair.optInt(1, -1)
            if (start >= 0 && end > start) out += start until end
        }
        return out
    }

    fun covers(ranges: List<IntRange>, offset: Int): Boolean =
        ranges.any { offset in it }
}
