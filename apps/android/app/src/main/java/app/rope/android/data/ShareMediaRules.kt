package app.rope.android.data

import java.io.File

/**
 * Telegram-like share of a downloaded IMAGE / VIDEO / FILE via ACTION_SEND.
 * FileProvider only exposes [MEDIA_DIR] under filesDir — not identity.
 * No LocalStore bump, no Go, not FCM.
 */
object ShareMediaRules {
    const val MEDIA_DIR = "media"
    const val ACTION = "Поделиться"
    const val CHOOSER = "Поделиться"
    const val SEND_ACTION = "android.intent.action.SEND"
    const val EXTRA_STREAM = "android.intent.extra.STREAM"
    const val MIN_BYTES = 8L
    const val AUTHORITY_SUFFIX = ".files"

    fun canShare(msg: ChatMessage): Boolean =
        !msg.deleted &&
            (msg.kind == MessageKind.IMAGE ||
                msg.kind == MessageKind.VIDEO ||
                msg.kind == MessageKind.FILE) &&
            !msg.localPath.isNullOrBlank()

    fun authority(packageName: String): String = packageName + AUTHORITY_SUFFIX

    fun mimeFor(msg: ChatMessage, file: File): String {
        val extra = runCatching { MediaPayload.parse(msg.extra).mime }.getOrNull()
        val clean = extra?.trim().orEmpty()
        if (clean.isNotEmpty() &&
            '/' in clean &&
            '\n' !in clean &&
            '\r' !in clean &&
            '\u0000' !in clean
        ) {
            return clean
        }
        return when (msg.kind) {
            MessageKind.IMAGE -> when (file.extension.lowercase()) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                "gif" -> "image/gif"
                else -> "image/jpeg"
            }
            MessageKind.VIDEO -> "video/mp4"
            else -> "application/octet-stream"
        }
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
