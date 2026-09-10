package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.VideoNoteMuteRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoNoteMuteRulesTest {
    @Test
    fun inheritStoreAndMuteCycle() {
        assertEquals(6, LocalStore.VERSION)
        assertTrue(VideoNoteMuteRules.toggle(false))
        assertFalse(VideoNoteMuteRules.toggle(true))
        assertEquals(1f, VideoNoteMuteRules.volume(false), 0.0001f)
        assertEquals(0f, VideoNoteMuteRules.volume(true), 0.0001f)
        assertEquals("Выключить звук", VideoNoteMuteRules.contentDescription(false))
        assertEquals("Включить звук", VideoNoteMuteRules.contentDescription(true))
        assertFalse(VideoNoteMuteRules.contentDescription(false).contains("FCM", ignoreCase = true))
    }
}
