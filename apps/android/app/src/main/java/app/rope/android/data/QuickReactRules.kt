package app.rope.android.data

/**
 * Telegram-like quick reaction: the first emoji on the message tap strip.
 * Default is today's ❤️. LocalStore stays v6.
 */
object QuickReactRules {
    const val DEFAULT = "❤️"
    const val TITLE = "Быстрая реакция"
    val CHOICES: List<String> = ReactionPayload.EMOJIS

    fun parse(raw: String?): String {
        val v = raw?.trim().orEmpty()
        return if (v in CHOICES) v else DEFAULT
    }

    fun tray(favorite: String?): List<String> {
        val fav = parse(favorite)
        return listOf(fav) + CHOICES.filter { it != fav }
    }

    fun hint(): String =
        "Первый значок на сообщении. Двойной тап не трогаем."
}
