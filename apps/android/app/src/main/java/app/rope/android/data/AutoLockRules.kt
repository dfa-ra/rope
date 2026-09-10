package app.rope.android.data

/**
 * Idle auto-lock after the app is backgrounded. Hides chats until Continue.
 * Not the PIN/biometric app-lock, not envelope crypto.
 */
object AutoLockRules {
    const val KEY = "auto_lock_ms"
    const val TITLE = "Автоблокировка"
    const val UNLOCK = "Продолжить"
    const val BODY = "Чаты скрыты после свёртывания."
    const val OFF = -1L
    const val IMMEDIATE = 0L

    data class Option(val ms: Long, val label: String)

    val OPTIONS: List<Option> = listOf(
        Option(OFF, "Выкл"),
        Option(IMMEDIATE, "Сразу"),
        Option(60_000L, "1 мин"),
        Option(300_000L, "5 мин"),
        Option(900_000L, "15 мин"),
        Option(3_600_000L, "1 ч"),
    )

    fun parse(raw: String?): Long {
        if (raw.isNullOrBlank()) return OFF
        if ('\n' in raw || '\r' in raw || '\u0000' in raw) return OFF
        val v = raw.trim().toLongOrNull() ?: return OFF
        return OPTIONS.find { it.ms == v }?.ms ?: OFF
    }

    fun normalize(ms: Long): Long = OPTIONS.find { it.ms == ms }?.ms ?: OFF

    fun enabled(ms: Long): Boolean = normalize(ms) != OFF

    fun label(ms: Long): String = OPTIONS.find { it.ms == normalize(ms) }?.label ?: OPTIONS.first().label

    fun shouldLock(
        timeoutMs: Long,
        lastBackgroundedAt: Long,
        now: Long,
        signedIn: Boolean,
        inCall: Boolean,
    ): Boolean {
        if (!signedIn || inCall) return false
        if (!enabled(timeoutMs)) return false
        if (lastBackgroundedAt <= 0L) return false
        return now - lastBackgroundedAt >= timeoutMs
    }

    fun hint(): String =
        "После свёртывания спрятать чаты. Код-пароль — отдельно."
}
