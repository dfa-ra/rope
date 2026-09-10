package app.rope.android.update

import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardOpenOption

/**
 * PackageInstaller sessions must read the GitHub APK from app-private
 * `cache/updates/` (same tree FileProvider XML names). The APK must be a
 * direct child — nested `updates/sub/` paths and symlinks fail closed.
 * [open] re-checks [allow] so a swap between check and read cannot follow
 * a link. Not envelope crypto.
 */
object ApkInstallRules {
    const val UPDATES_DIR = "updates"
    const val MIN_BYTES = 1024L

    fun allow(file: File, updatesDir: File): Boolean {
        try {
            if (Files.isSymbolicLink(file.toPath())) return false
        } catch (_: Exception) {
            return false
        }
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

    /**
     * Bytes for PackageInstaller / FileProvider. Fail closed if [allow]
     * disagrees before or after the NOFOLLOW open (TOCTOU).
     */
    fun open(file: File, updatesDir: File): InputStream? {
        if (!allow(file, updatesDir)) return null
        val stream = try {
            Files.newInputStream(file.toPath(), StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)
        } catch (_: Exception) {
            return null
        }
        if (!allow(file, updatesDir)) {
            try {
                stream.close()
            } catch (_: Exception) {
            }
            return null
        }
        return stream
    }

    /**
     * Map a FileProvider path (`/updates/<apk>`) to a direct child of
     * [updatesDir]. Nested segments and `..` fail closed.
     */
    fun fileForProviderPath(path: String?, updatesDir: File): File? {
        val raw = path?.trim('/') ?: return null
        val parts = raw.split('/')
        if (parts.size != 2) return null
        if (parts[0] != UPDATES_DIR) return null
        val name = parts[1]
        if (name.isEmpty() || name == "." || name == "..") return null
        if (name.indexOf('\n') >= 0 || name.indexOf('\r') >= 0 || name.indexOf('\u0000') >= 0) {
            return null
        }
        if ('/' in name || '\\' in name) return null
        return File(updatesDir, name)
    }
        file.canonicalPath
    } catch (_: Exception) {
        null
    }
}
