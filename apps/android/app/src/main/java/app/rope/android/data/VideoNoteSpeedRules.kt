package app.rope.android.data

/**
 * 1x / 1.5x / 2x on in-thread video notes (кружок). Same cycle as voice.
 * Not CallVideoRenderer. No FCM.
 */
object VideoNoteSpeedRules {
    val SPEEDS: List<Float> = VoicePlayback.SPEEDS

    fun clamp(speed: Float): Float = VoicePlayback.clampSpeed(speed)

    fun next(current: Float): Float = VoicePlayback.nextSpeed(current)

    fun label(speed: Float): String = VoicePlayback.speedLabel(speed)
}
