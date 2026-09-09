package app.rope.android.data

/**
 * Telegram-like message text size. Kv-only. Scales Compose fontScale,
 * not layout density, so paddings and logo colors stay put.
 * LocalStore schema stays v6.
 */
object FontScaleRules {
    const val SMALL = "s"
    const val NORMAL = "m"
    const val LARGE = "l"
    const val XL = "xl"
    const val DEFAULT = NORMAL

    data class Preset(val id: String, val label: String, val factor: Float)

    val PRESETS = listOf(
        Preset(SMALL, "Мелкий", 0.85f),
        Preset(NORMAL, "Обычный", 1.0f),
        Preset(LARGE, "Крупный", 1.15f),
        Preset(XL, "Очень крупный", 1.3f),
    )

    fun parse(raw: String?): String {
        val id = raw?.trim()?.lowercase().orEmpty()
        return PRESETS.firstOrNull { it.id == id }?.id ?: DEFAULT
    }

    fun factor(raw: String?): Float =
        PRESETS.first { it.id == parse(raw) }.factor

    fun hint(): String =
        "Только размер шрифта. Отступы и цвета логотипа не меняются."

    fun sample(): String = "Пример: привет, это размер текста в чате."
}
