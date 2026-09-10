package app.rope.android.data

import java.io.File

/**
 * Telegram-like save photo/video into the device gallery (MediaStore).
 * Files stay under [MEDIA_DIR]; identity never lands in shared storage.
 * No LocalStore bump, no Go, not FCM, not Downloads.
 */
object SaveGalleryRules {
    const val MEDIA_DIR = "media"
    const val ACTION = "Сохранить"
    const val NOTICE = "Сохранено в галерею"
    const val MIN_BYTES = 8L
    const val PICTURES = "Pictures/Rope/"
    const val MOVIES = "Movies/Rope/"

    fun canSave(msg: ChatMessage): Boolean =
        !msg.deleted &&
            (msg.kind == MessageKind.IMAGE || msg.kind == MessageKind.VIDEO) &&
            !msg.localPath.isNullOrBlank()

    fun isVideo(msg: ChatMessage): Boolean = msg.kind == MessageKind.VIDEO

    fun relativePath(video: Boolean): String = if (video) MOVIES else PICTURES

    fun relativePath(msg: ChatMessage): String = relativePath(isVideo(msg))

    fun mimeFor(msg: ChatMessage, file: File): String {
        val extra = runCatching { MediaPayload.parse(msg.extra).mime }.getOrNull()
        sanitizedMime(extra)?.let { return it }
        return when {
            isVideo(msg) -> "video/mp4"
            file.extension.lowercase() == "png" -> "image/png"
            file.extension.lowercase() == "webp" -> "image/webp"
            file.extension.lowercase() == "gif" -> "image/gif"
            else -> "image/jpeg"
        }
    }

    fun sanitizedMime(raw: String?): String? {
        val v = raw ?: return null
        if ('\n' in v || '\r' in v || '\u0000' in v) return null
        val clean = v.trim()
        return clean.takeIf { it.isNotEmpty() && '/' in it }
    }

    fun displayName(file: File): String = file.name

    fun saveFile(msg: ChatMessage, mediaDir: File): File? {
        if (!canSave(msg)) return null
        val path = msg.localPath ?: return null
        val file = File(path)
        return file.takeIf { allow(it, mediaDir) }
    }

    fun allow(file: File, mediaDir: File): Boolean {
        if (!file.isFile || file.length() < MIN_BYTES) return false
        val name = file.name
        if (name.indexOf('\n') >= 0 || name.indexOf('\r') >= 0 || name.indexOf('\u0000') >= 0) {
            return false
        }
        if ('/' in name || '\\' in name) return false
        val root = canonical(mediaDir) ?: return false
        val target = canonical(file) ?: return false
        val prefix = if (root.endsWith(File.separatorChar)) root else root + File.separator
        return target.startsWith(prefix)
    }

    private fun canonical(file: File): String? = try {
        file.canonicalPath
    } catch (_: Exception) {
        null
    }
}
