package app.rope.android.data

/**
 * In-thread video playback speed. Same 1x / 1.5x / 2x cycle as voice notes.
 * Not CallVideoRenderer. No FCM.
 */
object VideoSpeedRules {
    val SPEEDS: List<Float> = VoicePlayback.SPEEDS

    fun clamp(speed: Float): Float = VoicePlayback.clampSpeed(speed)

    fun next(current: Float): Float = VoicePlayback.nextSpeed(current)

    fun label(speed: Float): String = VoicePlayback.speedLabel(speed)
}
