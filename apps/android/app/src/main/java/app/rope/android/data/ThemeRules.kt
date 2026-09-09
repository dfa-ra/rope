package app.rope.android.data

/**
 * Telegram-like follow-system appearance. Kv stores LIGHT / DARK / SYSTEM.
 * Unset still snapshots from Configuration so first launch matches today.
 * LocalStore schema stays v6.
 */
object ThemeRules {
    const val SYSTEM_LABEL = "Система"

    fun parseStored(raw: String?, defaultDark: Boolean): ThemeMode =
        when (raw?.trim()?.lowercase()) {
            "light" -> ThemeMode.LIGHT
            "dark" -> ThemeMode.DARK
            "system", "auto" -> ThemeMode.SYSTEM
            else -> if (defaultDark) ThemeMode.DARK else ThemeMode.LIGHT
        }

    fun isDark(mode: ThemeMode, systemDark: Boolean): Boolean = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }

    fun statusLine(mode: ThemeMode): String = when (mode) {
        ThemeMode.DARK ->
            "Тема: тёмная · иконка солнца в шапке включает светлую"
        ThemeMode.LIGHT ->
            "Тема: светлая · иконка луны в шапке включает тёмную"
        ThemeMode.SYSTEM ->
            "Тема: как в системе · тёмная или светлая по телефону"
    }

    fun signedOutContentDescription(mode: ThemeMode): String = when (mode) {
        ThemeMode.DARK -> "Светлая тема"
        ThemeMode.LIGHT -> "Тёмная тема"
        ThemeMode.SYSTEM -> "Тема системы"
    }
}
