package app.rope.android.data

import java.text.BreakIterator
import java.util.Locale

/**
 * Telegram-like large emoji-only bubbles: 1–3 emoji clusters, no other text.
 * No protocol change; LocalStore stays v6.
 */
object EmojiBubbleRules {
    const val MAX = 3
    const val SP_ONE = 56
    const val SP_TWO = 44
    const val SP_THREE = 36

    fun applies(kind: MessageKind, text: String): Boolean =
        (kind == MessageKind.TEXT || kind == MessageKind.GROUP_TEXT) && count(text) != null

    fun fontSp(count: Int): Int = when (count) {
        1 -> SP_ONE
        2 -> SP_TWO
        3 -> SP_THREE
        else -> 16
    }

    fun count(text: String): Int? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val clusters = graphemes(trimmed)
        if (clusters.size !in 1..MAX) return null
        if (clusters.any { !isEmojiCluster(it) }) return null
        return clusters.size
    }

    fun graphemes(text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        val it = BreakIterator.getCharacterInstance(Locale.ROOT)
        it.setText(text)
        val out = mutableListOf<String>()
        var start = it.first()
        var end = it.next()
        while (end != BreakIterator.DONE) {
            out += text.substring(start, end)
            start = end
            end = it.next()
        }
        return out
    }

    fun isEmojiCluster(cluster: String): Boolean {
        if (cluster.isEmpty()) return false
        var i = 0
        var sawCore = false
        while (i < cluster.length) {
            val cp = cluster.codePointAt(i)
            if (!isEmojiPart(cp)) return false
            if (isCoreEmoji(cp)) sawCore = true
            i += Character.charCount(cp)
        }
        return sawCore
    }

    fun isEmojiPart(cp: Int): Boolean = isCoreEmoji(cp) || isEmojiJoin(cp)

    fun isEmojiJoin(cp: Int): Boolean = when (cp) {
        0x200D, 0xFE0E, 0xFE0F, 0x20E3 -> true
        in 0x1F3FB..0x1F3FF -> true
        in 0xE0020..0xE007F -> true
        in 0xFE00..0xFE0F -> true
        else -> false
    }

    fun isCoreEmoji(cp: Int): Boolean = when (cp) {
        in 0x1F000..0x1FAFF -> true
        in 0x1F1E6..0x1F1FF -> true
        in 0x2600..0x27BF -> true
        in 0x2300..0x23FF -> true
        in 0x2B00..0x2BFF -> true
        in 0x2190..0x21FF -> true
        in 0x25A0..0x25FF -> true
        in 0x2934..0x2935 -> true
        0x00A9, 0x00AE, 0x203C, 0x2049, 0x2122, 0x2139, 0x24C2,
        0x3030, 0x303D, 0x3297, 0x3299, 0x2B50, 0x2B55,
        -> true
        else -> false
    }
}
