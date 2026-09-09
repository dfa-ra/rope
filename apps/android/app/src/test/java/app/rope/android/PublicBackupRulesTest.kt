package app.rope.android

import app.rope.android.update.PublicBackupRules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PublicBackupRulesTest {
    @Test
    fun identityNeverGoesToPublicDownloads() {
        assertFalse(PublicBackupRules.allowIdentityDump)
        assertFalse(PublicBackupRules.allowAutoRestore)
        assertTrue(PublicBackupRules.allowApkCopy)
    }
}
