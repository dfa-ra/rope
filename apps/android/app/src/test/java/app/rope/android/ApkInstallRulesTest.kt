package app.rope.android

import app.rope.android.update.ApkInstallRules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class ApkInstallRulesTest {
    @Test
    fun allowOnlyRopeApkUnderUpdates() {
        val root = createTempDirectory("rope-apk-path").toFile()
        try {
            val updates = File(root, ApkInstallRules.UPDATES_DIR).apply { mkdirs() }
            val ok = File(updates, "rope-0.1.5.apk")
            ok.writeBytes(ByteArray(ApkInstallRules.MIN_BYTES.toInt() + 8))
            assertTrue(ApkInstallRules.allow(ok, updates))

            val small = File(updates, "rope-0.1.6.apk")
            small.writeBytes(ByteArray(8))
            assertFalse(ApkInstallRules.allow(small, updates))

            val other = File(root, "rope-0.1.5.apk")
            other.writeBytes(ByteArray(ApkInstallRules.MIN_BYTES.toInt() + 8))
            assertFalse(ApkInstallRules.allow(other, updates))

            val wrong = File(updates, "evil.apk")
            wrong.writeBytes(ByteArray(ApkInstallRules.MIN_BYTES.toInt() + 8))
            assertFalse(ApkInstallRules.allow(wrong, updates))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectCrLfNameAndSymlinkEscape() {
        val root = createTempDirectory("rope-apk-escape").toFile()
        try {
            val updates = File(root, ApkInstallRules.UPDATES_DIR).apply { mkdirs() }
            val outside = File(root, "rope-0.1.5.apk")
            outside.writeBytes(ByteArray(ApkInstallRules.MIN_BYTES.toInt() + 8))
            val link = File(updates, "rope-0.1.5.apk")
            java.nio.file.Files.createSymbolicLink(link.toPath(), outside.toPath())
            assertFalse(ApkInstallRules.allow(link, updates))

            val nl = File(updates, "rope-0.1.5.apk\n")
            nl.writeBytes(ByteArray(ApkInstallRules.MIN_BYTES.toInt() + 8))
            assertFalse(ApkInstallRules.allow(nl, updates))
        } finally {
            root.deleteRecursively()
        }
    }
}
