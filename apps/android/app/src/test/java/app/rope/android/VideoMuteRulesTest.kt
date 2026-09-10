package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.VideoMuteRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoMuteRulesTest {
    @Test
    fun inheritStoreAndMuteCycle() {
        assertEquals(6, LocalStore.VERSION)
        assertTrue(VideoMuteRules.toggle(false))
        assertFalse(VideoMuteRules.toggle(true))
        assertEquals(1f, VideoMuteRules.volume(false), 0.0001f)
        assertEquals(0f, VideoMuteRules.volume(true), 0.0001f)
        assertEquals("звук", VideoMuteRules.label(false))
        assertEquals("без звука", VideoMuteRules.label(true))
        assertEquals("Выключить звук", VideoMuteRules.contentDescription(false))
        assertEquals("Включить звук", VideoMuteRules.contentDescription(true))
        assertFalse(VideoMuteRules.label(false).contains("FCM", ignoreCase = true))
    }
}
