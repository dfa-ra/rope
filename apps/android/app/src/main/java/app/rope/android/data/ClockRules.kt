package app.rope.android.data

/**
 * Telegram-like 24-hour clock. Kv-only. Default on matches today's HH:mm.
 * 12-hour drops leading zero (14:05 → 2:05). LocalStore schema stays v6.
 */
object ClockRules {
    const val SECTION = "Время"
    const val TITLE = "24 часа"

    @Volatile
    var hour24: Boolean = true

    fun hint(): String =
        "В чатах и в списке. Цвета логотипа не меняются."

    fun hm(hourOfDay: Int, minute: Int, hour24: Boolean): String {
        if (hour24) return "%02d:%02d".format(hourOfDay, minute)
        val h = when {
            hourOfDay == 0 || hourOfDay == 12 -> 12
            hourOfDay > 12 -> hourOfDay - 12
            else -> hourOfDay
        }
        return "%d:%02d".format(h, minute)
    }
}
