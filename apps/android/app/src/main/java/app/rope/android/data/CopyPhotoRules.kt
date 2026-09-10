package app.rope.android.data

import java.io.File

/**
 * Telegram-like copy photo onto the system clipboard as an image URI.
 * FileProvider only exposes [MEDIA_DIR] under filesDir — not identity.
 * No LocalStore bump, no Go, not FCM.
 */
object CopyPhotoRules {
    const val MEDIA_DIR = "media"
    const val CLIP_LABEL = "image"
    const val URI_LIST_MIME = "text/uri-list"
    const val MIN_BYTES = 8L
    const val AUTHORITY_SUFFIX = ".files"

    fun canCopyImage(msg: ChatMessage): Boolean =
        !msg.deleted &&
            msg.kind == MessageKind.IMAGE &&
            !msg.localPath.isNullOrBlank()

    fun authority(packageName: String): String = packageName + AUTHORITY_SUFFIX

    fun mimeFor(file: File): String = when (file.extension.lowercase()) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        else -> "image/jpeg"
    }

    fun clipMimeTypes(file: File): Array<String> = arrayOf(mimeFor(file), URI_LIST_MIME)

    fun clipFile(msg: ChatMessage, mediaDir: File): File? {
        if (!canCopyImage(msg)) return null
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
