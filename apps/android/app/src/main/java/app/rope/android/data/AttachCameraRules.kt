package app.rope.android.data

import android.net.Uri

/**
 * Composer still + file video from the attach sheet.
 * Third camera path: not 1:1 calls, not кружок.
 */
object AttachCameraRules {
    const val CACHE_DIR = "attach-cam"
    const val FILE_PROVIDER_PATH = "attach-cam/"
    const val FILE_PREFIX = "attach-"
    const val MAX_VIDEO_MS = 240_000L
    const val MIN_VIDEO_MS = 400L
    const val HOLD_VIDEO_MS = 350L
    const val MAX_PREVIEW_EDGE = 1920
    const val DEFAULT_BACK = true
    const val KIND_IMAGE = "image"
    const val KIND_VIDEO = "video"
    const val UNAVAILABLE_NOTICE = "Камера недоступна"
    const val ALBUM_FULL_NOTICE = "До 10 фото или видео"
    const val HOLD_HINT = "Удерживайте для видео"
    const val FLASH_OFF = "off"
    const val FLASH_ON = "on"
    const val FLASH_AUTO = "auto"
    const val FLASH_TORCH = "torch"

    data class Size(val width: Int, val height: Int)

    fun fileProviderAuthority(packageName: String): String = "$packageName.files"

    fun cacheFileName(id: String, video: Boolean): String =
        "$FILE_PREFIX$id.${if (video) "mp4" else "jpg"}"

    fun albumEligible(kind: String): Boolean = VideoRules.albumEligible(kind)

    fun cameraDeniedNotice(): String = VideoCallRules.cameraDeniedNotice()

    fun micDeniedNotice(): String = VideoCallRules.micDeniedNotice()

    fun showTile(
        hasCall: Boolean,
        recordingVoice: Boolean,
        recordingNote: Boolean,
    ): Boolean = !hasCall && !recordingVoice && !recordingNote

    fun canOpen(
        hasCall: Boolean,
        recordingVoice: Boolean,
        recordingNote: Boolean,
        attachOpen: Boolean,
    ): Boolean = showTile(hasCall, recordingVoice, recordingNote) && !attachOpen

    fun canStartVideoNote(
        hasCall: Boolean,
        recordingVoice: Boolean,
        recordingNote: Boolean,
        attachOpen: Boolean,
    ): Boolean = !hasCall && !recordingVoice && !recordingNote && !attachOpen

    fun canStartVoice(recordingVoice: Boolean, recordingNote: Boolean, attachOpen: Boolean): Boolean =
        !recordingVoice && !recordingNote && !attachOpen

    fun canStartCall(attachOpen: Boolean): Boolean = !attachOpen

    fun mergeStaged(
        existing: List<String>,
        incoming: List<String>,
        max: Int = AlbumRules.MAX_PHOTOS,
    ): List<String> = (existing + incoming).distinct().take(max)

    fun canCaptureMore(pendingCount: Int, max: Int = AlbumRules.MAX_PHOTOS): Boolean =
        pendingCount < max

    fun isHoldVideo(heldMs: Long): Boolean = heldMs >= HOLD_VIDEO_MS

    fun isAttachCamPath(path: String?): Boolean {
        val p = path.orEmpty()
        return p.contains("/$CACHE_DIR/") || p.contains("$CACHE_DIR/")
    }

    fun isAttachCamUri(uri: Uri): Boolean = isAttachCamPath(uri.path)

    fun isPreferredAspect(width: Int, height: Int): Boolean {
        if (width <= 0 || height <= 0) return false
        val r = width.toFloat() / height.toFloat()
        val a = kotlin.math.abs
        return a(r - 16f / 9f) < 0.08f ||
            a(r - 4f / 3f) < 0.08f ||
            a(r - 9f / 16f) < 0.08f ||
            a(r - 3f / 4f) < 0.08f
    }

    fun pickPreview(sizes: List<Size>, maxEdge: Int = MAX_PREVIEW_EDGE): Size? {
        val fit = sizes.filter { it.width in 1..maxEdge && it.height in 1..maxEdge }
        val pool = fit.ifEmpty { sizes }
        if (pool.isEmpty()) return null
        val preferred = pool.filter { isPreferredAspect(it.width, it.height) }
        return (preferred.ifEmpty { pool }).maxByOrNull { it.width * it.height }
    }

    fun pickPicture(sizes: List<Size>, maxEdge: Int = MAX_PREVIEW_EDGE): Size? = pickPreview(sizes, maxEdge)

    fun pickBackCamera(count: Int, facingBackAt: (Int) -> Boolean): Int {
        if (count <= 0) return -1
        for (i in 0 until count) {
            if (facingBackAt(i)) return i
        }
        return 0
    }

    fun cyclePhotoFlash(current: String, supported: Collection<String>): String {
        val order = listOf(FLASH_OFF, FLASH_ON, FLASH_AUTO).filter { it in supported }
        if (order.isEmpty()) return current
        val idx = order.indexOf(current)
        return order[(idx + 1).mod(order.size)]
    }

    fun videoFlash(supported: Collection<String>): String? =
        when {
            FLASH_TORCH in supported -> FLASH_TORCH
            FLASH_OFF in supported -> FLASH_OFF
            else -> null
        }

    fun showFlash(supported: Collection<String>): Boolean =
        supported.any { it != FLASH_OFF }
}
