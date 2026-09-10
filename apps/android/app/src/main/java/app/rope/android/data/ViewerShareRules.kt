package app.rope.android.data

import java.io.File

/**
 * System share (ACTION_SEND) of the current photo/video in the fullscreen
 * viewer. Distinct from in-chat Переслать and from text «Поделиться».
 * FileProvider only exposes [MEDIA_DIR] under filesDir. LocalStore stays v6.
 */
object ViewerShareRules {
    const val ACTION = "Поделиться"
    const val CHOOSER = "Поделиться"
    const val SEND_ACTION = "android.intent.action.SEND"
    const val EXTRA_STREAM = "android.intent.extra.STREAM"
    const val MEDIA_DIR = "media"
    const val AUTHORITY_SUFFIX = ".files"
    const val MIN_BYTES = 8L

    fun canShare(msg: ChatMessage): Boolean =
        !msg.deleted &&
            (msg.kind == MessageKind.IMAGE || msg.kind == MessageKind.VIDEO) &&
            !msg.localPath.isNullOrBlank()

    fun current(album: List<ChatMessage>, page: Int, fallback: ChatMessage): ChatMessage =
        album.getOrNull(page) ?: fallback

    fun authority(packageName: String): String = packageName + AUTHORITY_SUFFIX

    fun mimeFor(file: File): String = when (file.extension.lowercase()) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "mp4", "m4v" -> "video/mp4"
        "webm" -> "video/webm"
        "3gp" -> "video/3gpp"
        else -> if (file.extension.lowercase() in setOf("jpg", "jpeg")) "image/jpeg" else "application/octet-stream"
    }

    fun shareFile(msg: ChatMessage, mediaDir: File): File? {
        if (!canShare(msg)) return null
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
