package app.rope.android.data

/**
 * Telegram-like scheduled night theme: dark from 22:00 to 07:00.
 * Manual theme chips turn the schedule off. LocalStore stays v6.
 */
object NightSchedRules {
    const val START_HOUR = 22
    const val END_HOUR = 7
    const val TITLE = "Ночь с 22:00"

    fun hour(hour24: Int): Int = ((hour24 % 24) + 24) % 24

    fun isNight(hour24: Int): Boolean {
        val h = hour(hour24)
        return h >= START_HOUR || h < END_HOUR
    }

    fun scheduledTheme(hour24: Int): ThemeMode =
        if (isNight(hour24)) ThemeMode.DARK else ThemeMode.LIGHT

    fun displayed(enabled: Boolean, hour24: Int, stored: ThemeMode): ThemeMode =
        if (enabled) scheduledTheme(hour24) else stored

    fun hint(): String =
        "Тёмная с 22:00 до 07:00. Ручная тема выключает расписание."
}
