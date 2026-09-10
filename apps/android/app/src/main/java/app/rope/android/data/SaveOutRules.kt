package app.rope.android.data

import java.io.File

/**
 * Settings toggle: copy outgoing photos/videos into the device gallery.
 * Distinct from manual save-gallery and incoming auto-download. Default off.
 * Identity stays out of shared storage. Not envelope crypto.
 */
object SaveOutRules {
    const val LABEL = "Сохранять исходящие в галерею"
    const val HINT =
        "Фото и видео, которые вы отправляете, копируются в галерею. Голос и файлы нет."
    const val KEY = "save_outgoing"
    const val MEDIA_DIR = "media"
    const val PICTURES = "Pictures/Rope/"
    const val MOVIES = "Movies/Rope/"
    const val MIN_BYTES = 8L

    fun enabled(raw: String?): Boolean = raw == "1"

    fun storeValue(on: Boolean): String = if (on) "1" else "0"

    fun shouldSave(enabled: Boolean, kind: String): Boolean =
        enabled && (kind == "image" || kind == "video")

    fun isVideo(kind: String): Boolean = kind == "video"

    fun relativePath(video: Boolean): String = if (video) MOVIES else PICTURES

    fun sanitizedMime(raw: String?): String? {
        val v = raw ?: return null
        if ('\n' in v || '\r' in v || '\u0000' in v) return null
        val clean = v.trim()
        return clean.takeIf { it.isNotEmpty() && '/' in it }
    }

    fun mimeFor(kind: String, mime: String, file: File): String {
        sanitizedMime(mime)?.let { return it }
        return when {
            isVideo(kind) -> "video/mp4"
            file.extension.lowercase() == "png" -> "image/png"
            file.extension.lowercase() == "webp" -> "image/webp"
            file.extension.lowercase() == "gif" -> "image/gif"
            else -> "image/jpeg"
        }
    }

    fun displayName(file: File): String {
        val name = file.name
        if (name.indexOf('\n') >= 0 || name.indexOf('\r') >= 0 || name.indexOf('\u0000') >= 0) {
            return fallbackName(file)
        }
        if ('/' in name || '\\' in name) return fallbackName(file)
        return name.ifBlank { fallbackName(file) }
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

    private fun fallbackName(file: File): String {
        val ext = file.extension.lowercase().filter { it.isLetterOrDigit() }.take(8)
        return if (ext.isEmpty()) "media" else "media.$ext"
    }

    private fun canonical(file: File): String? = try {
        file.canonicalPath
    } catch (_: Exception) {
        null
    }
}
