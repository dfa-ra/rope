package app.rope.android.media

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import app.rope.android.data.GallerySaveRules
import java.io.File

/** Copies a downloaded photo/video into the public gallery. API 29+. */
object GallerySave {
    fun copy(context: Context, file: File, mime: String, displayName: String): Boolean {
        if (!GallerySaveRules.supported(Build.VERSION.SDK_INT)) return false
        if (!file.isFile || file.length() <= 0L) return false
        val video = GallerySaveRules.isVideoMime(mime)
        val resolver = context.contentResolver
        val collection = if (video) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(
                MediaStore.MediaColumns.MIME_TYPE,
                mime.ifBlank { if (video) "video/mp4" else "image/jpeg" },
            )
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                if (video) "${Environment.DIRECTORY_MOVIES}/Rope" else "${Environment.DIRECTORY_PICTURES}/Rope",
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: return false
        return try {
            resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            } ?: return false
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } catch (_: Exception) {
            resolver.delete(uri, null, null)
            false
        }
    }
}
