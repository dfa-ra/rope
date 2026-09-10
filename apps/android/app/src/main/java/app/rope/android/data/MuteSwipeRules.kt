package app.rope.android.data

/**
 * Chat-list right-swipe to mute / unmute. Left-swipe stays archive
 * ([ArchiveSwipeRules]). Units are dp.
 */
object MuteSwipeRules {
    const val SLOP_DP = 12f
    const val COMMIT_DP = 48f
    const val MAX_DP = 72f

    fun canSwipe(id: String?, header: Boolean): Boolean =
        !header && !id.isNullOrBlank() && !SavedMessagesRules.isSaved(id)

    fun stillSettled(dxDp: Float, dyDp: Float): Boolean =
        kotlin.math.abs(dxDp) < SLOP_DP && kotlin.math.abs(dyDp) < SLOP_DP

    /** Vertical scroll (or left swipe) wins — do not consume the pointer. */
    fun shouldAbort(dxDp: Float, dyDp: Float): Boolean {
        if (stillSettled(dxDp, dyDp)) return false
        if (dxDp < -SLOP_DP) return true
        return kotlin.math.abs(dyDp) > kotlin.math.abs(dxDp)
    }

    /** Rightward horizontal drag past slop, not dominated by vertical. */
    fun shouldLock(dxDp: Float, dyDp: Float): Boolean {
        if (stillSettled(dxDp, dyDp)) return false
        return dxDp >= SLOP_DP && kotlin.math.abs(dxDp) >= kotlin.math.abs(dyDp)
    }

    fun offset(dxDp: Float): Float = dxDp.coerceIn(0f, MAX_DP)

    fun shouldCommit(offsetDp: Float): Boolean = offsetDp >= COMMIT_DP

    fun crossedCommit(previousDp: Float, nowDp: Float): Boolean =
        !shouldCommit(previousDp) && shouldCommit(nowDp)

    fun progress(offsetDp: Float): Float =
        (kotlin.math.abs(offsetDp) / COMMIT_DP).coerceIn(0f, 1f)

    fun iconAlpha(progress: Float): Float = progress.coerceIn(0f, 1f)

    fun iconScale(progress: Float): Float =
        if (progress >= 1f) 1.12f else 0.72f + 0.28f * progress.coerceIn(0f, 1f)
}
