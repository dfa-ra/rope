package app.rope.android

import app.rope.android.update.AppRelease
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppReleaseTest {
    @Test
    fun parseApkNames() {
        assertEquals("0.1.5", AppRelease.parseApkVersion("rope-0.1.5-debug.apk"))
        assertEquals("0.1.5", AppRelease.parseApkVersion("rope-0.1.5.apk"))
        assertEquals(null, AppRelease.parseApkVersion("rope-server-linux-amd64"))
    }

    @Test
    fun newerThanInstalled() {
        assertTrue(AppRelease.isNewer("0.1.5", "0.1.4-debug"))
        assertFalse(AppRelease.isNewer("0.1.4", "0.1.4-debug"))
        assertFalse(AppRelease.isNewer("0.1.3", "0.1.4"))
    }

    @Test
    fun pickDebugApkWhenPreferred() {
        val names = listOf("rope-server-linux-amd64", "rope-0.1.5-debug.apk", "SHA256SUMS")
        assertEquals("rope-0.1.5-debug.apk", AppRelease.pickApkAsset(names, preferDebug = true))
    }

    @Test
    fun parseApkVersionRejectsCrLf() {
        assertEquals(null, AppRelease.parseApkVersion("rope-0.1.5.apk\n"))
        assertEquals(null, AppRelease.parseApkVersion("rope-0.1.5.apk\r"))
        assertEquals(null, AppRelease.parseApkVersion("rope-0.1.5.apk\u0000"))
        assertEquals(null, AppRelease.parseApkVersion(" rope-0.1.5.apk"))
        assertEquals(
            null,
            AppRelease.pickApkAsset(listOf("rope-0.1.5.apk\n", "rope-0.1.5-debug.apk\r"), preferDebug = true),
        )
        assertEquals(
            "rope-0.1.5.apk",
            AppRelease.pickApkAsset(listOf("rope-0.1.5.apk\n", "rope-0.1.5.apk"), preferDebug = false),
        )
    }
}
