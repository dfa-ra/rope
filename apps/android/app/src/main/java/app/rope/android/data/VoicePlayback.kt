package app.rope.android.data

/** Pure helpers for voice-bubble progress, speed, seek, and bars. Safe without MediaPlayer. */
object VoicePlayback {
    const val BARS = 22
    const val SPEED_1X = 1.0f
    const val SPEED_1_5X = 1.5f
    const val SPEED_2X = 2.0f

    val SPEEDS: List<Float> = listOf(SPEED_1X, SPEED_1_5X, SPEED_2X)

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

    fun clampSpeed(speed: Float): Float = when {
        speed >= 1.75f -> SPEED_2X
        speed >= 1.25f -> SPEED_1_5X
        else -> SPEED_1X
    }

    fun nextSpeed(current: Float): Float {
        val i = SPEEDS.indexOf(clampSpeed(current))
        return SPEEDS[(i + 1) % SPEEDS.size]
    }

    fun speedLabel(speed: Float): String = when (clampSpeed(speed)) {
        SPEED_2X -> "2x"
        SPEED_1_5X -> "1.5x"
        else -> "1x"
    }

    fun seekMs(fraction: Float, durationMs: Long): Long {
        if (durationMs <= 0L) return 0L
        return (fraction.coerceIn(0f, 1f) * durationMs).toLong().coerceIn(0L, durationMs)
    }

    fun seekMsAt(x: Float, width: Float, durationMs: Long): Long {
        if (width <= 0f) return 0L
        return seekMs(x / width, durationMs)
    }

    fun encodeWaveform(bars: List<Float>, count: Int = BARS): List<Int> {
        val src = if (bars.isEmpty()) List(count) { 0.28f } else bars
        val n = count.coerceAtLeast(1)
        return resample(src, n).map { (it.coerceIn(0f, 1f) * 31f).toInt().coerceIn(0, 31) }
    }

    fun decodeWaveform(levels: List<Int>, bars: Int = BARS): List<Float> {
        if (levels.isEmpty()) return emptyList()
        val n = bars.coerceAtLeast(1)
        val floats = levels.map { it.coerceIn(0, 31) / 31f }
        return resample(floats, n).map { it.coerceIn(0.12f, 1f) }
    }

    fun barsFromAmplitudes(samples: List<Int>, bars: Int = BARS): List<Float> {
        if (samples.isEmpty()) return emptyList()
        val n = bars.coerceAtLeast(1)
        val peak = samples.maxOrNull()?.coerceAtLeast(1) ?: 1
        val norm = samples.map { (it.coerceAtLeast(0).toFloat() / peak).coerceIn(0f, 1f) }
        return resample(norm, n).map { (0.18f + it * 0.82f).coerceIn(0.18f, 1f) }
    }

    fun resolveBars(levels: List<Int>, seed: String, bars: Int = BARS): List<Float> {
        val decoded = decodeWaveform(levels, bars)
        return if (decoded.size == bars.coerceAtLeast(1) && decoded.any { it > 0.2f }) {
            decoded
        } else {
            waveform(seed, bars)
        }
    }

    fun litBarIndex(fraction: Float, barCount: Int): Int {
        if (barCount <= 0 || fraction <= 0f) return -1
        return ((barCount - 1) * fraction.coerceIn(0f, 1f)).toInt().coerceIn(0, barCount - 1)
    }

    private fun resample(src: List<Float>, n: Int): List<Float> {
        if (n <= 0) return emptyList()
        if (src.isEmpty()) return List(n) { 0.28f }
        if (src.size == n) return src
        if (src.size == 1) return List(n) { src[0] }
        return List(n) { i ->
            val t = i.toFloat() / (n - 1).coerceAtLeast(1)
            val pos = t * (src.lastIndex)
            val lo = pos.toInt().coerceIn(0, src.lastIndex)
            val hi = (lo + 1).coerceAtMost(src.lastIndex)
            val f = pos - lo
            src[lo] * (1f - f) + src[hi] * f
        }
    }
}
