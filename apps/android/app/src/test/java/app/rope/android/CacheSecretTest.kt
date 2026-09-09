package app.rope.android

import app.rope.android.provision.CacheSecret
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CacheSecretTest {
    @Test
    fun wipeDeletesFile() {
        val dir = File(System.getProperty("java.io.tmpdir"), "rope-cache-secret-${System.nanoTime()}")
        assertTrue(dir.mkdirs())
        try {
            val pem = File(dir, "key.pem")
            pem.writeText("-----BEGIN OPENSSH PRIVATE KEY-----\nsecret\n")
            assertTrue(pem.exists())
            assertTrue(CacheSecret.wipe(pem))
            assertFalse(pem.exists())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun wipeMissingFileIsOk() {
        val missing = File(System.getProperty("java.io.tmpdir"), "rope-missing-${System.nanoTime()}.pem")
        assertFalse(missing.exists())
        assertTrue(CacheSecret.wipe(missing))
    }

    @Test
    fun wipeStaleSshPemClearsWellKnownName() {
        val dir = File(System.getProperty("java.io.tmpdir"), "rope-stale-ssh-${System.nanoTime()}")
        assertTrue(dir.mkdirs())
        try {
            File(dir, CacheSecret.STALE_SSH_PEM).writeText("OLDKEY")
            val orphan = File(dir, "${CacheSecret.TEMP_PREFIX}orphan${CacheSecret.TEMP_SUFFIX}")
            orphan.writeText("ORPHAN")
            File(dir, "keep.txt").writeText("ok")
            assertTrue(CacheSecret.wipeStaleSshPem(dir))
            assertFalse(File(dir, CacheSecret.STALE_SSH_PEM).exists())
            assertFalse(orphan.exists())
            assertTrue(File(dir, "keep.txt").exists())
        } finally {
            dir.deleteRecursively()
        }
    }
}
