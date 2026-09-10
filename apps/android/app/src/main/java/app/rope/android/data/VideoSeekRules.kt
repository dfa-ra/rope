package app.rope.android.data

/**
 * Scrub an in-thread video bubble. Not voice seek, not the Calls overlay.
 */
object VideoSeekRules {
    fun clamp(positionMs: Long, durationMs: Long): Long {
        if (durationMs <= 0L) return 0L
        return positionMs.coerceIn(0L, durationMs)
    }

    fun progress(positionMs: Long, durationMs: Long): Float {
        if (durationMs <= 0L) return 0f
        return clamp(positionMs, durationMs).toFloat() / durationMs.toFloat()
    }

    fun fromProgress(fraction: Float, durationMs: Long): Long {
        if (durationMs <= 0L) return 0L
        return clamp((fraction.coerceIn(0f, 1f) * durationMs).toLong(), durationMs)
    }

    fun fromPointerX(x: Float, width: Float, durationMs: Long): Long {
        if (width <= 0f) return 0L
        return fromProgress(x / width, durationMs)
    }

    fun canScrub(playing: Boolean, durationMs: Long): Boolean =
        playing && durationMs > 0L
}
