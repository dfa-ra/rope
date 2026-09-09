package app.rope.android

import app.rope.android.update.AppRelease
import app.rope.android.update.DeviceBackup
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceBackupTest {
    private val xor: (ByteArray) -> ByteArray = { src ->
        ByteArray(src.size) { i -> (src[i].toInt() xor 0x5A).toByte() }
    }

    private fun sample() = DeviceBackup(
        identity = byteArrayOf(1, 2, 3, 9),
        profileJson = """{"host":"10.0.0.8","port":8443}""",
        githubToken = "ghp_test",
        sshJson = """{"host":"10.0.0.8","sshPort":22,"user":"root","listenPort":8443}""",
    )

    @Test
    fun roundtripKeepsIdentityAndProfile() {
        val backup = sample()
        val parsed = DeviceBackup.parse(backup.toBytes())
        assertArrayEquals(backup.identity, parsed.identity)
        assertEquals(backup.profileJson, parsed.profileJson)
        assertEquals(backup.githubToken, parsed.githubToken)
        assertEquals(backup.sshJson, parsed.sshJson)
        assertEquals("rope-device.backup", DeviceBackup.FILE_NAME)
    }

    @Test
    fun sealedBytesHideKeysAndOpen() {
        val backup = sample()
        val sealed = backup.toSealedBytes(xor)
        assertTrue(DeviceBackup.isSealed(sealed))
        assertTrue(DeviceBackup.allowAutoRestore(sealed))
        val asText = String(sealed, Charsets.ISO_8859_1)
        assertFalse(asText.contains("ghp_test"))
        assertFalse(asText.contains("identity"))
        val opened = DeviceBackup.open(sealed, xor)
        assertArrayEquals(backup.identity, opened.identity)
        assertEquals(backup.githubToken, opened.githubToken)
        assertEquals(backup.sshJson, opened.sshJson)
    }

    @Test
    fun cleartextJsonIsNotAutoRestored() {
        val raw = sample().toBytes()
        assertFalse(DeviceBackup.isSealed(raw))
        assertFalse(DeviceBackup.allowAutoRestore(raw))
        val opened = DeviceBackup.open(raw) { error("must not decrypt v1 json") }
        assertEquals("ghp_test", opened.githubToken)
    }

    @Test
    fun robkMagicIsNotDeviceBackupSeal() {
        val robk = byteArrayOf(0x52, 0x4F, 0x42, 0x4B) + byteArrayOf(1, 2, 3)
        assertFalse(DeviceBackup.isSealed(robk))
        assertFalse(DeviceBackup.allowAutoRestore(robk))
    }

    @Test
    fun twoOneIsNewerThanTwoZero() {
        assertTrue(AppRelease.isNewer("0.2.1", "0.2.0-debug"))
        assertEquals(0, AppRelease.compareSemver("0.2.1", "0.2.1"))
    }
}
