package app.rope.android.data

/**
 * Telegram-like in-chat video scrubber. Maps pointer/fraction onto
 * [VideoView] milliseconds. Not envelope crypto; no CallVideoRenderer.
 */
object VideoSeekRules {
    fun seekMs(fraction: Float, durationMs: Long): Long {
        if (durationMs <= 0L) return 0L
        return (fraction.coerceIn(0f, 1f) * durationMs).toLong().coerceIn(0L, durationMs)
    }

    fun seekMsAt(x: Float, width: Float, durationMs: Long): Long {
        if (width <= 0f) return 0L
        return seekMs(x / width, durationMs)
    }

    fun fraction(positionMs: Long, durationMs: Long): Float {
        if (durationMs <= 0L) return 0f
        return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }

    fun resolvedDuration(playerDurationMs: Long, payloadDurationMs: Long): Long = when {
        playerDurationMs > 0L -> playerDurationMs
        payloadDurationMs > 0L -> payloadDurationMs
        else -> 0L
    }

    fun clock(positionMs: Long, durationMs: Long): String {
        val pos = positionMs.coerceAtLeast(0L)
        val dur = durationMs.coerceAtLeast(0L)
        return "${MediaPayload.formatDuration(pos)} / ${MediaPayload.formatDuration(dur)}"
    }

    fun showsScrubber(active: Boolean, durationMs: Long): Boolean =
        active && durationMs > 0L
}
