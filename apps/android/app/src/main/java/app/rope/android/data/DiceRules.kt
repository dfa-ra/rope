package app.rope.android.data

data class DiceKind(
    val emoji: String,
    val faces: Int,
    val label: String,
)

/**
 * Attach-sheet **Кубик**. Sends a Telegram-like dice as TEXT (emoji + face).
 * No new MessageKind, no stickers, no LocalStore bump.
 */
object DiceRules {
    const val LABEL = "Кубик"
    val KINDS = listOf(
        DiceKind("🎲", 6, "Кубик"),
        DiceKind("🎯", 6, "Дартс"),
        DiceKind("🏀", 5, "Баскетбол"),
        DiceKind("⚽", 5, "Футбол"),
        DiceKind("🎳", 6, "Боулинг"),
    )

    fun kind(emoji: String?): DiceKind? {
        val e = emoji?.trim().orEmpty()
        if (e.isEmpty() || e.any { it == '\n' || it == '\r' || it == '\u0000' }) return null
        return KINDS.find { it.emoji == e }
    }

    fun clamp(kind: DiceKind, value: Int): Int? =
        value.takeIf { it in 1..kind.faces }

    fun text(kind: DiceKind, value: Int): String? {
        val face = clamp(kind, value) ?: return null
        val body = "${kind.emoji} $face"
        if (body.any { it == '\n' || it == '\r' || it == '\u0000' }) return null
        return body
    }

    fun parse(raw: String?): Pair<DiceKind, Int>? {
        val t = raw?.trim().orEmpty()
        if (t.isEmpty() || t.any { it == '\r' || it == '\u0000' }) return null
        val parts = t.split(' ', limit = 2)
        if (parts.size != 2) return null
        val kind = kind(parts[0]) ?: return null
        val face = parts[1].toIntOrNull() ?: return null
        val clamped = clamp(kind, face) ?: return null
        return kind to clamped
    }

    fun isDice(text: String?): Boolean = parse(text) != null
}
