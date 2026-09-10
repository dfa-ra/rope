package app.rope.android

import app.rope.android.update.PublicDownloads
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PublicDownloadsTest {
    @Test
    fun safeNameAcceptsApkAssets() {
        assertTrue(PublicDownloads.safeName("rope-0.3.50.apk"))
        assertTrue(PublicDownloads.safeName("rope-0.3.50-debug.apk"))
    }

    @Test
    fun safeNameRejectsPathAndControl() {
        assertFalse(PublicDownloads.safeName("../identity.ropi"))
        assertFalse(PublicDownloads.safeName("a/b.apk"))
        assertFalse(PublicDownloads.safeName("a\\b.apk"))
        assertFalse(PublicDownloads.safeName("rope.apk\n.apk"))
        assertFalse(PublicDownloads.safeName("rope.apk\r\nX"))
        assertFalse(PublicDownloads.safeName("rope apk"))
        assertFalse(PublicDownloads.safeName(""))
        assertFalse(PublicDownloads.safeName("."))
        assertFalse(PublicDownloads.safeName(".."))
    }
}
