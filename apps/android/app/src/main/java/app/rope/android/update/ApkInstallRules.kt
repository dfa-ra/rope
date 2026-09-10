package app.rope.android.update

import java.io.File

/**
 * PackageInstaller sessions must read the GitHub APK from app-private
 * `cache/updates/` (same tree FileProvider exposes). The APK must be a
 * direct child — nested `updates/sub/` paths fail closed. Not envelope crypto.
 */
object ApkInstallRules {
    const val UPDATES_DIR = "updates"
    const val MIN_BYTES = 1024L

    fun allow(file: File, updatesDir: File): Boolean {
        if (!file.isFile || file.length() < MIN_BYTES) return false
        val name = file.name
        if (name.indexOf('\n') >= 0 || name.indexOf('\r') >= 0 || name.indexOf('\u0000') >= 0) {
            return false
        }
        if ('/' in name || '\\' in name) return false
        if (AppRelease.parseApkVersion(name) == null) return false
        val root = canonical(updatesDir) ?: return false
        val target = canonical(file) ?: return false
        val parent = File(target).parent ?: return false
        return parent == root
    }

    private fun canonical(file: File): String? = try {
        file.canonicalPath
    } catch (_: Exception) {
        null
    }
}
