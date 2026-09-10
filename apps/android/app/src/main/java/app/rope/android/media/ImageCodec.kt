package app.rope.android.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import app.rope.android.data.VideoRules
import java.io.ByteArrayOutputStream
import java.io.File

object ImageCodec {
    const val MAX_EDGE = 2048
    const val PREVIEW_EDGE = 1200

    fun extensionFor(mime: String, name: String): String {
        val m = mime.lowercase()
        if (m.startsWith("video/")) return VideoRules.extension(mime, name)
        val fromMime = when {
            "jpeg" in m || "jpg" in m -> "jpg"
            "png" in m -> "png"
            "webp" in m -> "webp"
            "gif" in m -> "gif"
            "heic" in m || "heif" in m -> "jpg"
            "mp4" in m || "m4a" in m || "aac" in m -> "m4a"
            else -> ""
        }
        if (fromMime.isNotEmpty()) return fromMime
        val raw = name.substringAfterLast('.').lowercase().filter { it.isLetterOrDigit() }
        return raw.takeIf { it.length in 1..8 } ?: "bin"
    }

    fun fileName(objectId: String, mime: String, name: String): String {
        val id = objectId.filter { it.isLetterOrDigit() || it == '-' }.ifBlank { "obj" }
        return "$id.${extensionFor(mime, name)}"
    }

    fun persist(dir: File, objectId: String, mime: String, name: String, bytes: ByteArray): File {
        dir.mkdirs()
        val dest = File(dir, fileName(objectId, mime, name))
        dest.writeBytes(bytes)
        return dest
    }

    fun normalizeForSend(
        bytes: ByteArray,
        mime: String,
        maxEdge: Int = MAX_EDGE,
        quality: Int = 85,
    ): Pair<ByteArray, String> {
        if (!mime.startsWith("image/")) return bytes to mime
        val src = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes to mime
        val scaled = scale(src, maxEdge.coerceAtLeast(1))
        val out = ByteArrayOutputStream()
        val q = quality.coerceIn(40, 100)
        val ok = scaled.compress(Bitmap.CompressFormat.JPEG, q, out)
        if (!ok || out.size() == 0) return bytes to mime
        return out.toByteArray() to "image/jpeg"
    }

    fun decodePreview(path: String): Bitmap? {
        val file = File(path)
        if (!file.isFile || file.length() < 8) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return BitmapFactory.decodeFile(path)
        }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, PREVIEW_EDGE)
        }
        return BitmapFactory.decodeFile(path, opts)
    }

    fun scale(src: Bitmap, maxEdge: Int): Bitmap {
        val w = src.width
        val h = src.height
        val edge = maxOf(w, h)
        if (edge <= maxEdge || w <= 0 || h <= 0) return src
        val ratio = maxEdge.toFloat() / edge
        return Bitmap.createScaledBitmap(src, (w * ratio).toInt().coerceAtLeast(1), (h * ratio).toInt().coerceAtLeast(1), true)
    }

    fun compressLinkThumb(
        bytes: ByteArray,
        maxEdge: Int = 320,
        quality: Int = 70,
        maxBytes: Int = 64 * 1024,
    ): ByteArray? {
        val src = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val scaled = scale(src, maxEdge)
        var q = quality
        while (q >= 40) {
            val out = ByteArrayOutputStream()
            val ok = scaled.compress(Bitmap.CompressFormat.JPEG, q, out)
            if (ok && out.size() in 1..maxBytes) return out.toByteArray()
            q -= 10
        }
        return null
    }

    fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        var w = width
        var h = height
        while (w / sample > maxEdge || h / sample > maxEdge) {
            sample *= 2
        }
        return sample.coerceAtLeast(1)
    }
}
