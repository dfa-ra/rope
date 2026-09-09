package app.rope.android.data

/**
 * Telegram-like round video note (кружок). Inner JSON kind is `video_note`;
 * envelope type stays 2 so Go never sees it. Not an in-call camera path.
 */
object VideoNoteRules {
    const val KIND = "video_note"
    const val MAX_MS = 60_000L
    const val MIN_MS = 400L
    const val SIZE_PX = 480
    const val FPS = 24
    const val VIDEO_BITRATE_BPS = 1_200_000
    const val AUDIO_BITRATE_BPS = 64_000
    const val DISPLAY_DP = 220

    fun isNote(kind: String): Boolean = kind == KIND

    fun isNote(msg: ChatMessage): Boolean =
        msg.kind == MessageKind.VIDEO_NOTE ||
            (msg.extra.isNotBlank() && runCatching { isNote(MediaPayload.parse(msg.extra).kind) }.getOrDefault(false))

    fun preview(durationMs: Long): String =
        if (durationMs > 0L) {
            "Видеосообщение · ${MediaPayload.formatDuration(durationMs)}"
        } else {
            "Видеосообщение"
        }

    fun fitsDuration(durationMs: Long): Boolean = durationMs in MIN_MS..MAX_MS

    fun albumEligible(kind: String): Boolean = false

    /**
     * Camera1 [setDisplayOrientation] for a front/back camera given the
     * display's rotation in degrees (0/90/180/270).
     */
    fun previewOrientation(facingFront: Boolean, cameraOrientation: Int, displayRotationDeg: Int): Int {
        val degrees = when (displayRotationDeg) {
            90 -> 90
            180 -> 180
            270 -> 270
            else -> 0
        }
        val orient = cameraOrientation.floorMod(360)
        return if (facingFront) {
            (360 - ((orient + degrees) % 360)) % 360
        } else {
            (orient - degrees + 360) % 360
        }
    }

    /** [MediaRecorder.setOrientationHint] so the file plays upright. */
    fun recordingHint(facingFront: Boolean, cameraOrientation: Int): Int {
        val orient = cameraOrientation.floorMod(360)
        return if (facingFront) (360 - orient) % 360 else orient
    }

    fun pickFrontCamera(count: Int, facingAt: (Int) -> Boolean): Int {
        if (count <= 0) return -1
        for (i in 0 until count) {
            if (facingAt(i)) return i
        }
        return 0
    }

    private fun Int.floorMod(m: Int): Int {
        val r = this % m
        return if (r < 0) r + m else r
    }
}
