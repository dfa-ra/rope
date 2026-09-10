package app.rope.android.update

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

object PublicDownloads {
    /**
     * Shared Downloads filenames must be a single path segment. `File(dir, name)`
     * on API 28 and MediaStore DISPLAY_NAME must not see `/`, `\`, or CR/LF.
     * APK copies from [AppRelease] already match `rope-x.y.z.apk`.
     */
    fun safeName(name: String): Boolean {
        val t = name.trim()
        if (t.isEmpty() || t == "." || t == ".." || t.length > 128) return false
        return t.none { it == '/' || it == '\\' || it.isWhitespace() || it.isISOControl() }
    }

    fun write(context: Context, name: String, mime: String, bytes: ByteArray) {
        if (!safeName(name)) error("bad download name")
        if (Build.VERSION.SDK_INT >= 29) {
            val resolver = context.contentResolver
            resolver.delete(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                "${MediaStore.Downloads.DISPLAY_NAME}=?",
                arrayOf(name),
            )
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("не удалось сохранить $name в Загрузки")
            try {
                resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("не удалось записать $name")
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
            return
        }
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        dir.mkdirs()
        File(dir, name).writeBytes(bytes)
    }

    fun read(context: Context, name: String): ByteArray? {
        if (!safeName(name)) return null
        if (Build.VERSION.SDK_INT >= 29) {
            val resolver = context.contentResolver
            resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Downloads._ID),
                "${MediaStore.Downloads.DISPLAY_NAME}=?",
                arrayOf(name),
                "${MediaStore.Downloads.DATE_ADDED} DESC",
            )?.use { c ->
                if (c.moveToFirst()) {
                    val id = c.getLong(0)
                    val uri = android.content.ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                    return resolver.openInputStream(uri)?.use { it.readBytes() }
                }
            }
        }
        val legacy = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), name)
        return if (legacy.isFile) legacy.readBytes() else null
    }
}
