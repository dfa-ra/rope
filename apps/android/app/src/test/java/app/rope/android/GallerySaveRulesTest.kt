package app.rope.android

import app.rope.android.data.GallerySaveRules
import app.rope.android.data.MessageKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GallerySaveRulesTest {
    @Test
    fun onlyPhotosAndVideosWhenOn() {
        assertTrue(GallerySaveRules.shouldCopy(MessageKind.IMAGE, enabled = true))
        assertTrue(GallerySaveRules.shouldCopy(MessageKind.VIDEO, enabled = true))
        assertFalse(GallerySaveRules.shouldCopy(MessageKind.VOICE, enabled = true))
        assertFalse(GallerySaveRules.shouldCopy(MessageKind.FILE, enabled = true))
        assertFalse(GallerySaveRules.shouldCopy(MessageKind.VIDEO_NOTE, enabled = true))
        assertFalse(GallerySaveRules.shouldCopy(MessageKind.IMAGE, enabled = false))
    }

    @Test
    fun galleryWriteNeedsQ() {
        assertFalse(GallerySaveRules.supported(28))
        assertTrue(GallerySaveRules.supported(29))
        assertTrue(GallerySaveRules.isVideoMime("video/mp4"))
        assertFalse(GallerySaveRules.isVideoMime("image/jpeg"))
    }

    @Test
    fun displayNamePrefixesObjectId() {
        assertEquals("abcd1234-pic.jpg", GallerySaveRules.displayName("pic.jpg", "abcd1234ffff"))
        assertEquals("deadbeef-rope.jpg", GallerySaveRules.displayName("  ", "deadbeef"))
        assertTrue(GallerySaveRules.hint().contains("галере"))
        assertFalse(GallerySaveRules.hint().contains("FCM"))
    }
}
