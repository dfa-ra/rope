package app.rope.android.data

/**
 * Group info **Медленный режим** is a local send cooldown on this device
 * for the open group. Not disappear, not a group-wide relay timer.
 * ChatPrefs JSON field, LocalStore schema stays v6.
 */
object SlowModeRules {
    const val TITLE = "Медленный режим"
    const val HINT = "Пауза между отправками на этом телефоне. Не автоудаление."
    const val JSON_KEY = "slow_mode_sec"
    const val OFF = 0

    data class Chip(val seconds: Int, val label: String)

    val CHIPS = listOf(
        Chip(OFF, "Выкл"),
        Chip(10, "10с"),
        Chip(30, "30с"),
        Chip(60, "1м"),
    )

    fun normalize(seconds: Int): Int =
        CHIPS.find { it.seconds == seconds }?.seconds ?: OFF

    fun parseStored(raw: Int): Int = normalize(raw)

    fun canShow(isMember: Boolean): Boolean = isMember

    fun waitMs(cooldownSec: Int, lastSendMs: Long, nowMs: Long): Long {
        val sec = normalize(cooldownSec)
        if (sec <= 0 || lastSendMs <= 0L) return 0L
        val until = lastSendMs + sec * 1000L
        return (until - nowMs).coerceAtLeast(0L)
    }

    fun blockedNotice(remainingMs: Long): String {
        val sec = ((remainingMs + 999L) / 1000L).coerceAtLeast(1L)
        return "Подождите ещё $sec с"
    }
}
