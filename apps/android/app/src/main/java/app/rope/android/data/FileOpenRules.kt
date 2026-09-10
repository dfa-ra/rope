package app.rope.android.data

import java.io.File

/**
 * Telegram-like tap-to-open for document bubbles. Files live under app
 * `files/media/` (FileProvider `files-path`); APK updates stay in
 * `cache/updates/`. Not envelope crypto. No FCM.
 */
data class FileOpenTarget(
    val mime: String,
    val name: String,
    val path: String,
)

object FileOpenRules {
    const val FILES_DIR = "media"
    const val FALLBACK_MIME = "application/octet-stream"
    const val CHOOSER = "Открыть файл"

    fun canOpen(msg: ChatMessage): Boolean =
        !msg.deleted && msg.kind == MessageKind.FILE

    fun displayName(msg: ChatMessage): String {
        val fromPayload = runCatching { MediaPayload.parse(msg.extra).name }.getOrNull().orEmpty().trim()
        val raw = fromPayload.ifBlank { msg.text.trim() }.ifBlank { "файл" }
        return raw.substringAfterLast('/').substringAfterLast('\\').ifBlank { "файл" }
    }

    fun mime(msg: ChatMessage): String {
        val raw = runCatching { MediaPayload.parse(msg.extra).mime }.getOrNull().orEmpty()
        return sanitizeMime(raw)
    }

    fun sanitizeMime(raw: String): String {
        if (raw.any { it == '\n' || it == '\r' || it == '\u0000' }) return FALLBACK_MIME
        val m = raw.trim().lowercase()
        if (m.isEmpty() || m.any { it.isWhitespace() }) return FALLBACK_MIME
        val parts = m.split('/')
        if (parts.size != 2) return FALLBACK_MIME
        val ok = parts.all { p -> p.isNotEmpty() && p.all { ch -> ch.isLetterOrDigit() || ch in "!#$&^_.+-" } }
        return if (ok) m else FALLBACK_MIME
    }

    fun target(msg: ChatMessage, mediaDir: File): FileOpenTarget? {
        if (!canOpen(msg)) return null
        val path = msg.localPath ?: return null
        val file = File(path)
        if (!allow(file, mediaDir)) return null
        return FileOpenTarget(mime(msg), displayName(msg), file.canonicalPath)
    }

    fun allow(file: File, mediaDir: File): Boolean {
        if (!file.isFile || file.length() <= 0L) return false
        val name = file.name
        if (name.indexOf('\n') >= 0 || name.indexOf('\r') >= 0 || name.indexOf('\u0000') >= 0) return false
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
