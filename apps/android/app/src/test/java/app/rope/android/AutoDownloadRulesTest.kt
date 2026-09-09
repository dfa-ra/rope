package app.rope.android

import app.rope.android.data.AutoDownloadPrefs
import app.rope.android.data.AutoDownloadRules
import app.rope.android.data.MessageKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoDownloadRulesTest {
    @Test
    fun defaultsPrefetchPhotosNotVideoOrFiles() {
        val p = AutoDownloadPrefs()
        assertTrue(AutoDownloadRules.prefetch(MessageKind.IMAGE, p))
        assertFalse(AutoDownloadRules.prefetch(MessageKind.VIDEO, p))
        assertFalse(AutoDownloadRules.prefetch(MessageKind.FILE, p))
        assertTrue(AutoDownloadRules.prefetch(MessageKind.VOICE, p))
        assertTrue(AutoDownloadRules.prefetch(MessageKind.VIDEO_NOTE, p))
        assertFalse(AutoDownloadRules.prefetch(MessageKind.TEXT, p))
    }

    @Test
    fun jsonRoundTripAndBadInput() {
        val json = AutoDownloadRules.toJson(AutoDownloadPrefs(photos = false, videos = true, files = true))
        val parsed = AutoDownloadRules.parse(json)
        assertEquals(AutoDownloadPrefs(photos = false, videos = true, files = true), parsed)
        assertEquals(AutoDownloadPrefs(), AutoDownloadRules.parse(null))
        assertEquals(AutoDownloadPrefs(), AutoDownloadRules.parse("{"))
    }
}
