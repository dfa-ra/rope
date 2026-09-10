package app.rope.android.data

/**
 * Pause a locked in-composer voice take. Playback pause is VoicePlayer.
 * Not envelope crypto.
 */
object PauseRecRules {
    const val PAUSE = "Пауза"
    const val RESUME = "Продолжить"

    fun canPause(recording: Boolean, locked: Boolean): Boolean = recording && locked

    fun nextPaused(paused: Boolean): Boolean = !paused

    fun clockRuns(paused: Boolean): Boolean = !paused

    fun caption(paused: Boolean): String = if (paused) "запись на паузе" else "запись закреплена"
}
