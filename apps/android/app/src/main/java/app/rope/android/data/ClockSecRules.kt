package app.rope.android.data

/**
 * Telegram-like seconds on bubble clocks. Kv-only. Default off matches
 * today's HH:mm. LocalStore schema stays v6.
 */
object ClockSecRules {
    const val SECTION = "Время"
    const val TITLE = "Секунды"

    @Volatile
    var showSeconds: Boolean = false

    fun hint(): String =
        "На пузырях и в списке чатов. Цвета логотипа не меняются."

    fun hm(hourOfDay: Int, minute: Int, second: Int = 0, seconds: Boolean = false): String {
        val hm = "%02d:%02d".format(hourOfDay, minute)
        return if (seconds) "$hm:%02d".format(second) else hm
    }
}
