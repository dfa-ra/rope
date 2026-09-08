package app.rope.android

import app.rope.android.update.AppRelease
import app.rope.android.update.DeviceBackup
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceBackupTest {
    @Test
    fun roundtripKeepsIdentityAndProfile() {
        val backup = DeviceBackup(
            identity = byteArrayOf(1, 2, 3, 9),
            profileJson = """{"host":"10.0.0.8","port":8443}""",
            githubToken = "ghp_test",
            sshJson = """{"host":"10.0.0.8","sshPort":22,"user":"root","listenPort":8443}""",
        )
        val parsed = DeviceBackup.parse(backup.toBytes())
        assertArrayEquals(backup.identity, parsed.identity)
        assertEquals(backup.profileJson, parsed.profileJson)
        assertEquals(backup.githubToken, parsed.githubToken)
        assertEquals(backup.sshJson, parsed.sshJson)
        assertEquals("rope-device.backup", DeviceBackup.FILE_NAME)
    }

    @Test
    fun twoOneIsNewerThanTwoZero() {
        assertTrue(AppRelease.isNewer("0.2.1", "0.2.0-debug"))
        assertEquals(0, AppRelease.compareSemver("0.2.1", "0.2.1"))
    }
}
