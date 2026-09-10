package app.rope.android.data

/**
 * Telegram-like tap on a bubble clock expands to the full date.
 * LocalStore stays v6.
 */
object FullDateRules {
    fun label(ms: Long): String {
        if (ms <= 0L) return ""
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = ms }
        val hm = "%02d:%02d".format(
            cal.get(java.util.Calendar.HOUR_OF_DAY),
            cal.get(java.util.Calendar.MINUTE),
        )
        return "%02d.%02d.%04d %s".format(
            cal.get(java.util.Calendar.DAY_OF_MONTH),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.YEAR),
            hm,
        )
    }

    fun meta(
        status: MessageStatus,
        outgoing: Boolean,
        timestampMs: Long,
        edited: Boolean = false,
        now: Long = System.currentTimeMillis(),
        full: Boolean = false,
    ): String {
        val time = if (full) label(timestampMs) else MessageTime.label(timestampMs, now)
        val mark = ComposerRules.statusLabel(status, outgoing)
        val edit = if (edited) "изм." else ""
        return listOf(time, edit, mark).filter { it.isNotBlank() }.joinToString(" · ")
    }
}
