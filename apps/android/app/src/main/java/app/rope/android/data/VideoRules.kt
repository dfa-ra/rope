package app.rope.android.data

/**
 * Telegram-like in-chat video. Inner JSON is still type=2 ([MediaPayload.kind] =
 * `video`); Go never sees it. Compression is not crypto.
 */
object VideoRules {
    const val MAX_OBJECT_BYTES = 25 * 1024 * 1024
    const val TARGET_HEIGHT = 720
    const val FALLBACK_HEIGHT = 480
    const val AUDIO_BITRATE_BPS = 64_000
    const val HEADROOM = 0.90

    fun looksLikeVideo(name: String, mime: String): Boolean {
        val m = mime.lowercase()
        if (m.startsWith("video/")) return true
        val n = name.lowercase()
        return n.endsWith(".mp4") || n.endsWith(".mov") || n.endsWith(".m4v") ||
            n.endsWith(".webm") || n.endsWith(".3gp") || n.endsWith(".mkv") ||
            n.endsWith(".avi")
    }

    fun alreadyMp4(mime: String, name: String): Boolean {
        val m = mime.lowercase()
        val n = name.lowercase()
        return m == "video/mp4" || n.endsWith(".mp4") || n.endsWith(".m4v")
    }

    fun kind(mime: String, name: String): String = when {
        looksLikeVideo(name, mime) -> "video"
        mime.startsWith("image/") -> "image"
        mime.startsWith("audio/") -> "voice"
        else -> "file"
    }

    fun fitsCap(size: Long): Boolean = size in 1..MAX_OBJECT_BYTES.toLong()

    fun mustCompress(size: Long, mime: String, name: String): Boolean =
        !fitsCap(size) || !alreadyMp4(mime, name)

    fun preview(durationMs: Long): String =
        if (durationMs > 0L) "Видео · ${MediaPayload.formatDuration(durationMs)}" else "Видео"

    fun videoBitrateBps(durationMs: Long): Int {
        val sec = (durationMs / 1000.0).coerceAtLeast(1.0)
        val budget = (MAX_OBJECT_BYTES * 8.0 * HEADROOM / sec) - AUDIO_BITRATE_BPS
        return budget.toInt().coerceIn(250_000, 2_500_000)
    }

    fun extension(mime: String, name: String): String {
        val m = mime.lowercase()
        if (m.contains("webm")) return "webm"
        if (m.contains("3gp")) return "3gp"
        val raw = name.substringAfterLast('.').lowercase().filter { it.isLetterOrDigit() }
        if (raw == "webm" || raw == "3gp") return raw
        return "mp4"
    }

    /** Photos still album; each video is its own type=2 envelope. */
    fun albumEligible(kind: String): Boolean = kind == "image"
}
