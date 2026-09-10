package app.rope.android.update

import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import java.io.File

/**
 * FileProvider XML still names `cache/updates/`. openFile / query fail closed
 * unless [ApkInstallRules.allow] — nested paths, symlinks, and non-Rope APKs
 * in that tree are not readable through the grant. Not envelope crypto.
 */
class ApkFileProvider : FileProvider() {
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        if (!allowed(uri)) return null
        return super.query(uri, projection, selection, selectionArgs, sortOrder)
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        if (!mode.startsWith("r") || !allowed(uri)) return null
        return super.openFile(uri, "r")
    }

    private fun allowed(uri: Uri): Boolean {
        val ctx = context ?: return false
        val file = try {
            getFileForUri(uri)
        } catch (_: Exception) {
            return false
        }
        return ApkInstallRules.allow(file, File(ctx.cacheDir, ApkInstallRules.UPDATES_DIR))
    }
}
