package app.rope.android.data

/**
 * Telegram-like swipe-to-reply on a chat bubble. Units are dp (density-independent).
 * Horizontal lock is leftward only so LazyColumn vertical scroll and 0.3.2
 * tap / long-press stay unconsumed until the swipe actually starts.
 */
object SwipeToReplyRules {
    const val SLOP_DP = 12f
    const val COMMIT_DP = 48f
    const val MAX_DP = 72f

    fun canSwipe(msg: ChatMessage, selecting: Boolean): Boolean =
        ChatActions.canReply(msg) && !selecting

    fun canSwipeAlbum(members: List<ChatMessage>, selecting: Boolean): Boolean =
        !selecting && members.any { ChatActions.canReply(it) }

    fun replyTarget(members: List<ChatMessage>): ChatMessage? =
        members.firstOrNull { ChatActions.canReply(it) }

    /** Still a tap / long-press: movement has not left the slop box. */
    fun stillSettled(dxDp: Float, dyDp: Float): Boolean =
        kotlin.math.abs(dxDp) < SLOP_DP && kotlin.math.abs(dyDp) < SLOP_DP

    /** Vertical scroll (or right swipe) wins — do not consume the pointer. */
    fun shouldAbort(dxDp: Float, dyDp: Float): Boolean {
        if (stillSettled(dxDp, dyDp)) return false
        if (dxDp > SLOP_DP) return true
        return kotlin.math.abs(dyDp) > kotlin.math.abs(dxDp)
    }

    /** Leftward horizontal drag past slop, not dominated by vertical. */
    fun shouldLock(dxDp: Float, dyDp: Float): Boolean {
        if (stillSettled(dxDp, dyDp)) return false
        return dxDp <= -SLOP_DP && kotlin.math.abs(dxDp) >= kotlin.math.abs(dyDp)
    }

    fun offset(dxDp: Float): Float = dxDp.coerceIn(-MAX_DP, 0f)

    fun shouldCommit(offsetDp: Float): Boolean = offsetDp <= -COMMIT_DP

    fun crossedCommit(previousDp: Float, nowDp: Float): Boolean =
        !shouldCommit(previousDp) && shouldCommit(nowDp)

    fun progress(offsetDp: Float): Float =
        (kotlin.math.abs(offsetDp) / COMMIT_DP).coerceIn(0f, 1f)

    fun iconAlpha(progress: Float): Float = progress.coerceIn(0f, 1f)

    fun iconScale(progress: Float): Float =
        if (progress >= 1f) 1.12f else 0.72f + 0.28f * progress.coerceIn(0f, 1f)
}
