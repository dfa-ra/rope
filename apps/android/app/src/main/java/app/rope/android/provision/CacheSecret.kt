package app.rope.android.provision

import java.io.File

/**
 * Overwrite-then-unlink for cache files that briefly held a secret (SSH PEM).
 * Not envelope crypto. App-private cache is not a remote hole; leftover PEM
 * still should not sit on disk after auth.
 */
object CacheSecret {
    const val STALE_SSH_PEM = "ssh-key.pem"
    const val TEMP_PREFIX = "rope-ssh-"
    const val TEMP_SUFFIX = ".pem"

    fun wipe(file: File): Boolean {
        if (!file.exists()) return true
        try {
            val n = file.length()
            if (n > 0L) {
                file.outputStream().use { out ->
                    val buf = ByteArray(8192)
                    var left = n
                    while (left > 0) {
                        val chunk = minOf(left, buf.size.toLong()).toInt()
                        out.write(buf, 0, chunk)
                        left -= chunk
                    }
                    out.flush()
                }
            }
        } catch (_: Exception) {
            // still unlink
        }
        return file.delete() || !file.exists()
    }

    fun wipeStaleSshPem(cacheDir: File): Boolean {
        var ok = wipe(File(cacheDir, STALE_SSH_PEM))
        cacheDir.listFiles()?.forEach { f ->
            val name = f.name
            if (f.isFile && name.startsWith(TEMP_PREFIX) && name.endsWith(TEMP_SUFFIX)) {
                ok = wipe(f) && ok
            }
        }
        return ok
    }
}
