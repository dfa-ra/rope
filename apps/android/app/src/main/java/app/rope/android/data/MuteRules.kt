package app.rope.android.data

/**
 * Telegram-like mute durations for a chat. Stored on ChatPrefs JSON
 * (`mute_until_ms`); LocalStore schema stays v6.
 */
enum class MuteChoice {
    HOUR,
    EIGHT_HOURS,
    TWO_DAYS,
    FOREVER,
}

object MuteRules {
    const val HOUR_MS = 60L * 60L * 1000L
    const val EIGHT_HOURS_MS = 8L * HOUR_MS
    const val TWO_DAYS_MS = 2L * 24L * HOUR_MS

    val CHOICES: List<MuteChoice> = listOf(
        MuteChoice.HOUR,
        MuteChoice.EIGHT_HOURS,
        MuteChoice.TWO_DAYS,
        MuteChoice.FOREVER,
    )

    fun untilMs(choice: MuteChoice, nowMs: Long): Long = when (choice) {
        MuteChoice.HOUR -> nowMs + HOUR_MS
        MuteChoice.EIGHT_HOURS -> nowMs + EIGHT_HOURS_MS
        MuteChoice.TWO_DAYS -> nowMs + TWO_DAYS_MS
        MuteChoice.FOREVER -> 0L
    }

    fun active(muted: Boolean, muteUntilMs: Long, nowMs: Long): Boolean {
        if (!muted) return false
        if (muteUntilMs <= 0L) return true
        return nowMs < muteUntilMs
    }

    fun label(choice: MuteChoice): String = when (choice) {
        MuteChoice.HOUR -> "1 час"
        MuteChoice.EIGHT_HOURS -> "8 часов"
        MuteChoice.TWO_DAYS -> "2 дня"
        MuteChoice.FOREVER -> "Навсегда"
    }

    fun muteLabel(): String = "Без звука"

    fun unmuteLabel(): String = "Включить звук"
}

fun ChatPrefs.effectivelyMuted(nowMs: Long = System.currentTimeMillis()): Boolean =
    MuteRules.active(muted, muteUntilMs, nowMs)
