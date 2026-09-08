package app.rope.android.data

/** Pure helpers for voice-bubble progress. Safe to unit-test without MediaPlayer. */
object VoicePlayback {
    const val BARS = 22

    fun fraction(positionMs: Long, durationMs: Long): Float {
        if (durationMs <= 0L) return 0f
        return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }

    fun clock(positionMs: Long, durationMs: Long): String {
        val pos = positionMs.coerceAtLeast(0L)
        val dur = durationMs.coerceAtLeast(0L)
        return "${MediaPayload.formatDuration(pos)} / ${MediaPayload.formatDuration(dur)}"
    }

    fun resolvedDuration(playerDurationMs: Long, payloadDurationMs: Long): Long = when {
        playerDurationMs > 0L -> playerDurationMs
        payloadDurationMs > 0L -> payloadDurationMs
        else -> 0L
    }

    fun displayPosition(active: Boolean, positionMs: Long): Long =
        if (active) positionMs.coerceAtLeast(0L) else 0L

    /** Outgoing voice is still uploading or waiting for a server ack. */
    fun isSending(outgoing: Boolean, status: MessageStatus): Boolean =
        outgoing && status == MessageStatus.CREATED

    fun waveform(seed: String, bars: Int = BARS): List<Float> {
        var h = seed.hashCode()
        return List(bars.coerceAtLeast(1)) { i ->
            h = h * 31 + i
            0.28f + ((h ushr 8) and 0xFF) / 255f * 0.72f
        }
    }
}
