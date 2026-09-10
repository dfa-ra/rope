package app.rope.android.data

/**
 * Telegram-like replace-emoji-in-text. :) becomes a smiley on send.
 * Settings kv; not stickers, not envelope crypto.
 */
object EmojiReplaceRules {
    const val KEY = "emoji_replace"
    const val TITLE = "Заменять эмодзи"

    private val PAIRS: List<Pair<String, String>> = listOf(
        ":-)" to "🙂",
        ":-(" to "🙁",
        ":-D" to "😃",
        ":-P" to "😛",
        ":-p" to "😛",
        ";-)" to "😉",
        ":)" to "🙂",
        ":(" to "🙁",
        ":D" to "😃",
        ":P" to "😛",
        ":p" to "😛",
        ";)" to "😉",
        "<3" to "❤",
    )

    fun enabledFromKv(raw: String?): Boolean = raw != "0"

    fun apply(text: String, enabled: Boolean): String {
        if (!enabled || text.isEmpty()) return text
        var out = text
        PAIRS.forEach { (from, to) -> out = out.replace(from, to) }
        return out
    }

    fun hint(): String =
        ":) в тексте становится смайлом при отправке. Не стикеры."
}
