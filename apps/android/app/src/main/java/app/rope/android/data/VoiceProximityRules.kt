package app.rope.android.data

/**
 * Pause in-thread voice when the phone is at the ear (proximity).
 * Not call audio. LocalStore stays v6.
 */
enum class VoiceProximityAction {
    NONE,
    PAUSE,
    RESUME,
}

object VoiceProximityRules {
    const val NEAR_CM = 5f

    fun isNear(distance: Float, maxRange: Float): Boolean {
        if (distance < 0f) return false
        val cap = when {
            maxRange <= 0f -> NEAR_CM
            maxRange < NEAR_CM -> maxRange
            else -> NEAR_CM
        }
        return distance < cap
    }

    fun action(
        playing: Boolean,
        pausedByProximity: Boolean,
        near: Boolean,
    ): VoiceProximityAction = when {
        near && playing -> VoiceProximityAction.PAUSE
        !near && pausedByProximity -> VoiceProximityAction.RESUME
        else -> VoiceProximityAction.NONE
    }

    fun afterUserToggle(): Boolean = false
}
