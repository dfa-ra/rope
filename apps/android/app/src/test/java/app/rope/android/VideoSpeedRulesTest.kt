package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.VideoSpeedRules
import app.rope.android.data.VoicePlayback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VideoSpeedRulesTest {
    @Test
    fun inheritStoreAndVoiceCycle() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals(VoicePlayback.SPEEDS, VideoSpeedRules.SPEEDS)
        assertEquals(1.0f, VideoSpeedRules.clamp(0.5f), 0.0001f)
        assertEquals(1.5f, VideoSpeedRules.clamp(1.4f), 0.0001f)
        assertEquals(2.0f, VideoSpeedRules.clamp(2.4f), 0.0001f)
        assertEquals(1.5f, VideoSpeedRules.next(1.0f), 0.0001f)
        assertEquals(2.0f, VideoSpeedRules.next(1.5f), 0.0001f)
        assertEquals(1.0f, VideoSpeedRules.next(2.0f), 0.0001f)
        assertEquals("1x", VideoSpeedRules.label(1.0f))
        assertEquals("1.5x", VideoSpeedRules.label(1.5f))
        assertEquals("2x", VideoSpeedRules.label(2.0f))
        assertFalse(VideoSpeedRules.label(1.0f).contains("FCM", ignoreCase = true))
    }
}
