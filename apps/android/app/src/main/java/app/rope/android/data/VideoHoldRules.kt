package app.rope.android.data

/**
 * Press-and-hold on playing in-thread or fullscreen video plays at 2x
 * until release. A short tap is not 2x. Not the 1x/1.5x/2x chip.
 * Not CallVideoRenderer. LocalStore stays v6.
 */
object VideoHoldRules {
    const val HOLD_MS = 280L
    const val HOLD_SPEED = 2f
    const val BASE_SPEED = 1f
    const val LABEL = "2x"

    fun reachedHold(elapsedMs: Long): Boolean = elapsedMs >= HOLD_MS

    fun speed(holding: Boolean, playing: Boolean = true, base: Float = BASE_SPEED): Float =
        if (holding && playing) HOLD_SPEED else base

    fun chipVisible(holding: Boolean, playing: Boolean = true): Boolean =
        holding && playing
}
