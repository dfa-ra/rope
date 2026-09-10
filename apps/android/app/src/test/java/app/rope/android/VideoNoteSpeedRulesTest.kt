package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.VideoNoteSpeedRules
import app.rope.android.data.VoicePlayback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VideoNoteSpeedRulesTest {
    @Test
    fun inheritStoreAndVoiceCycle() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals(VoicePlayback.SPEEDS, VideoNoteSpeedRules.SPEEDS)
        assertEquals(1.0f, VideoNoteSpeedRules.clamp(0.5f), 0.0001f)
        assertEquals(1.5f, VideoNoteSpeedRules.clamp(1.4f), 0.0001f)
        assertEquals(2.0f, VideoNoteSpeedRules.clamp(2.4f), 0.0001f)
        assertEquals(1.5f, VideoNoteSpeedRules.next(1.0f), 0.0001f)
        assertEquals(2.0f, VideoNoteSpeedRules.next(1.5f), 0.0001f)
        assertEquals(1.0f, VideoNoteSpeedRules.next(2.0f), 0.0001f)
        assertEquals("1x", VideoNoteSpeedRules.label(1.0f))
        assertEquals("1.5x", VideoNoteSpeedRules.label(1.5f))
        assertEquals("2x", VideoNoteSpeedRules.label(2.0f))
        assertFalse(VideoNoteSpeedRules.label(1.0f).contains("FCM", ignoreCase = true))
    }
}
