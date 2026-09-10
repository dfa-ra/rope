package app.rope.android.data

/**
 * Long-press a reaction chip to see who set it. Tap still toggles.
 * Names are local display strings, not envelope crypto.
 */
object ReactionWhoRules {
    const val FALLBACK = "Участник"

    fun canShow(people: List<Reaction>): Boolean = people.isNotEmpty()

    fun names(people: List<Reaction>): List<String> =
        people.map { it.displayName.trim().ifBlank { FALLBACK } }

    fun title(emoji: String, count: Int): String {
        val mark = emoji.trim().ifBlank { "🙂" }
        return "$mark · $count"
    }
}
