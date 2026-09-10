package app.rope.android

import app.rope.android.update.ApkInstallRules
import org.junit.Assert.assertEquals
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

    @Test
    fun rejectNestedUpdatesSubdir() {
        val root = createTempDirectory("rope-apk-nest").toFile()
        try {
            val updates = File(root, ApkInstallRules.UPDATES_DIR).apply { mkdirs() }
            val nested = File(updates, "sub").apply { mkdirs() }
            val apk = File(nested, "rope-0.1.5.apk")
            apk.writeBytes(ByteArray(ApkInstallRules.MIN_BYTES.toInt() + 8))
            assertFalse(ApkInstallRules.allow(apk, updates))

            val deep = File(File(nested, "more").apply { mkdirs() }, "rope-0.1.5.apk")
            deep.writeBytes(ByteArray(ApkInstallRules.MIN_BYTES.toInt() + 8))
            assertFalse(ApkInstallRules.allow(deep, updates))

            val direct = File(updates, "rope-0.1.5.apk")
            direct.writeBytes(ByteArray(ApkInstallRules.MIN_BYTES.toInt() + 8))
            assertTrue(ApkInstallRules.allow(direct, updates))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectSameDirSymlinkAndOpenRechecksAllow() {
        val root = createTempDirectory("rope-apk-open").toFile()
        try {
            val updates = File(root, ApkInstallRules.UPDATES_DIR).apply { mkdirs() }
            val real = File(updates, "rope-0.1.5.apk")
            real.writeBytes(ByteArray(ApkInstallRules.MIN_BYTES.toInt() + 8))
            val link = File(updates, "rope-0.1.6.apk")
            java.nio.file.Files.createSymbolicLink(link.toPath(), real.toPath())
            assertFalse(ApkInstallRules.allow(link, updates))
            assertEquals(null, ApkInstallRules.open(link, updates))

            ApkInstallRules.open(real, updates)!!.use { input ->
                assertEquals(real.length(), input.readBytes().size.toLong())
            }
            val nested = File(File(updates, "sub").apply { mkdirs() }, "rope-0.1.5.apk")
            nested.writeBytes(ByteArray(ApkInstallRules.MIN_BYTES.toInt() + 8))
            assertEquals(null, ApkInstallRules.open(nested, updates))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun providerPathIsSingleUpdatesChild() {
        val root = createTempDirectory("rope-apk-uri").toFile()
        try {
            val updates = File(root, ApkInstallRules.UPDATES_DIR).apply { mkdirs() }
            val ok = File(updates, "rope-0.1.5.apk")
            assertEquals(ok, ApkInstallRules.fileForProviderPath("/updates/rope-0.1.5.apk", updates))
            assertEquals(ok, ApkInstallRules.fileForProviderPath("updates/rope-0.1.5.apk", updates))
            assertEquals(null, ApkInstallRules.fileForProviderPath("/updates/sub/rope-0.1.5.apk", updates))
            assertEquals(null, ApkInstallRules.fileForProviderPath("/updates/../rope-0.1.5.apk", updates))
            assertEquals(null, ApkInstallRules.fileForProviderPath("/updates/rope-0.1.5.apk\n", updates))
            assertEquals(null, ApkInstallRules.fileForProviderPath("/files/rope-0.1.5.apk", updates))
            assertEquals(null, ApkInstallRules.fileForProviderPath("/updates/.", updates))
        } finally {
            root.deleteRecursively()
        }
    }
}
