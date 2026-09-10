package app.rope.android.data

/**
 * In-thread voice-note playback volume. Chip next to 1x/1.5x/2x.
 * Persist in kv (`voice_vol`). LocalStore stays v6. Not FCM.
 */
object VoiceVolRules {
    const val KEY = "voice_vol"
    const val VOL_25 = 0.25f
    const val VOL_50 = 0.5f
    const val VOL_100 = 1.0f

    val LEVELS: List<Float> = listOf(VOL_25, VOL_50, VOL_100)

    fun clamp(volume: Float): Float = when {
        volume <= 0.375f -> VOL_25
        volume <= 0.75f -> VOL_50
        else -> VOL_100
    }

    fun next(current: Float): Float {
        val i = LEVELS.indexOf(clamp(current))
        return LEVELS[(i + 1) % LEVELS.size]
    }

    fun label(volume: Float): String = when (clamp(volume)) {
        VOL_25 -> "25%"
        VOL_50 -> "50%"
        else -> "100%"
    }

    fun stored(volume: Float): String = when (clamp(volume)) {
        VOL_25 -> "25"
        VOL_50 -> "50"
        else -> "100"
    }

    fun parse(raw: String?): Float {
        if (raw == null) return VOL_100
        if (raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0 || raw.indexOf('\u0000') >= 0) {
            return VOL_100
        }
        return when (raw.trim()) {
            "25" -> VOL_25
            "50" -> VOL_50
            "100" -> VOL_100
            else -> VOL_100
        }
    }
}
