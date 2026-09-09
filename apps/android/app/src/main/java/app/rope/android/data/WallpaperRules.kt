package app.rope.android.data

/**
 * Local chat wallpaper presets (kv-only). Logo / primary colors stay unchanged.
 * LocalStore schema stays v6.
 */
data class WallpaperPreset(
    val id: String,
    val label: String,
    val darkArgb: Int?,
    val lightArgb: Int?,
)

object WallpaperRules {
    const val DEFAULT = "default"
    const val GRAPHITE = "graphite"
    const val SLATE = "slate"
    const val PINE = "pine"
    const val DUSK = "dusk"

    val PRESETS: List<WallpaperPreset> = listOf(
        WallpaperPreset(DEFAULT, "Тема", null, null),
        WallpaperPreset(GRAPHITE, "Графит", 0xFF1C1C1F.toInt(), 0xFFE4E4E7.toInt()),
        WallpaperPreset(SLATE, "Сланец", 0xFF1E293B.toInt(), 0xFFE2E8F0.toInt()),
        WallpaperPreset(PINE, "Хвоя", 0xFF14261C.toInt(), 0xFFECF3EE.toInt()),
        WallpaperPreset(DUSK, "Закат", 0xFF1C1917.toInt(), 0xFFF5EDE6.toInt()),
    )

    fun parse(raw: String?): String {
        val id = raw?.trim().orEmpty().ifBlank { DEFAULT }
        return PRESETS.find { it.id == id }?.id ?: DEFAULT
    }

    fun argb(id: String, dark: Boolean): Int? {
        val preset = PRESETS.find { it.id == parse(id) } ?: return null
        return if (dark) preset.darkArgb else preset.lightArgb
    }

    fun label(id: String): String =
        PRESETS.find { it.id == parse(id) }?.label ?: PRESETS.first().label

    fun hint(): String =
        "Фон чата только на этом телефоне. Цвета логотипа не меняются."
}
