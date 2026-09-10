package app.rope.android.data

/**
 * Telegram-like outgoing bubble color. Default is today's graphite.
 * Incoming bubbles stay theme-neutral. LocalStore stays v6.
 */
object ChatColorRules {
    const val DEFAULT = ""
    const val TITLE = "Цвет пузырей"
    const val LABEL_DEFAULT = "Графит"

    data class Swatch(val id: String, val label: String, val darkArgb: Long, val lightArgb: Long)

    val OPTIONS: List<Swatch> = listOf(
        Swatch(DEFAULT, LABEL_DEFAULT, 0xFF3F3F46, 0xFF3F3F46),
        Swatch("blue", "Синий", 0xFF1D4ED8, 0xFF3B82F6),
        Swatch("teal", "Бирюза", 0xFF0F766E, 0xFF14B8A6),
        Swatch("violet", "Фиолет", 0xFF7C3AED, 0xFF8B5CF6),
        Swatch("rose", "Роза", 0xFFBE123C, 0xFFF43F5E),
    )

    fun parse(raw: String?): String {
        val v = raw?.trim()?.lowercase().orEmpty()
        return OPTIONS.find { it.id == v }?.id ?: DEFAULT
    }

    fun swatch(mode: String?): Swatch =
        OPTIONS.find { it.id == parse(mode) } ?: OPTIONS.first()

    fun label(mode: String?): String = swatch(mode).label

    fun outArgb(mode: String?, dark: Boolean): Long {
        val s = swatch(mode)
        return if (dark) s.darkArgb else s.lightArgb
    }

    fun outFgArgb(mode: String?, dark: Boolean): Long =
        if (parse(mode) == DEFAULT && dark) 0xFFF4F4F5 else 0xFFFFFFFF

    fun hint(): String =
        "Исходящие пузыри. Входящие не красятся. Не тема приложения."
}
