package app.rope.android.data

/**
 * Settings clock window that silences shade message alerts.
 * Not the night theme schedule. Incoming calls still ring. LocalStore kv.
 */
object QuietHoursRules {
    const val KEY = "quiet_hours"
    const val OFF = "off"
    const val TITLE = "Не беспокоить"

    data class Option(val id: String, val label: String, val startHour: Int, val endHour: Int)

    val OPTIONS: List<Option> = listOf(
        Option(OFF, "Выкл", -1, -1),
        Option("22-8", "22:00–08:00", 22, 8),
        Option("23-7", "23:00–07:00", 23, 7),
        Option("0-6", "00:00–06:00", 0, 6),
    )

    fun sanitize(raw: String?): String? {
        if (raw.isNullOrBlank()) return OFF
        if ('\n' in raw || '\r' in raw || '\u0000' in raw) return null
        val v = raw.trim()
        return OPTIONS.find { it.id == v }?.id
    }

    fun normalize(raw: String?): String = sanitize(raw) ?: OFF

    fun enabled(raw: String?): Boolean = normalize(raw) != OFF

    fun hour(hour24: Int): Int = ((hour24 % 24) + 24) % 24

    fun active(raw: String?, hour24: Int): Boolean {
        val opt = OPTIONS.find { it.id == normalize(raw) } ?: return false
        if (opt.id == OFF) return false
        val h = hour(hour24)
        return if (opt.startHour < opt.endHour) {
            h >= opt.startHour && h < opt.endHour
        } else {
            h >= opt.startHour || h < opt.endHour
        }
    }

    fun label(raw: String?): String =
        OPTIONS.find { it.id == normalize(raw) }?.label ?: OPTIONS.first().label

    fun hint(): String =
        "Тишина в шторке по часам. Тема «Ночь с 22:00» отдельно. Звонки звонят."
}
