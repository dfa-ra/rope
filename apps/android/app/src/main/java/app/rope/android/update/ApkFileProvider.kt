package app.rope.android.update

import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileNotFoundException

/**
 * FileProvider XML still names `cache/updates/`. query/openFile fail closed
 * unless the URI is a single Rope APK child that [ApkInstallRules.allow]
 * accepts. Nested paths and non-APK files in that tree are not readable.
 * Not envelope crypto.
 */
class ApkFileProvider : FileProvider() {
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        if (!allowed(uri)) return MatrixCursor(arrayOf())
        return super.query(uri, projection, selection, selectionArgs, sortOrder)
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (!mode.startsWith("r")) throw FileNotFoundException(uri.toString())
        val file = resolve(uri) ?: throw FileNotFoundException(uri.toString())
        val ctx = context ?: throw FileNotFoundException()
        val updates = File(ctx.cacheDir, ApkInstallRules.UPDATES_DIR)
        ApkInstallRules.open(file, updates)?.close() ?: throw FileNotFoundException(uri.toString())
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    private fun allowed(uri: Uri): Boolean {
        val file = resolve(uri) ?: return false
        val ctx = context ?: return false
        return ApkInstallRules.allow(file, File(ctx.cacheDir, ApkInstallRules.UPDATES_DIR))
    }

    private fun resolve(uri: Uri): File? {
        val ctx = context ?: return null
        val updates = File(ctx.cacheDir, ApkInstallRules.UPDATES_DIR)
        return ApkInstallRules.fileForProviderPath(uri.path, updates)
    }
}
