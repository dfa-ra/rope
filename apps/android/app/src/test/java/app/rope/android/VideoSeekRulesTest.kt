package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.VideoSeekRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoSeekRulesTest {
    @Test
    fun seekMapsFractionAndPointerX() {
        assertEquals(0L, VideoSeekRules.seekMs(-1f, 10_000))
        assertEquals(5_000L, VideoSeekRules.seekMs(0.5f, 10_000))
        assertEquals(10_000L, VideoSeekRules.seekMs(2f, 10_000))
        assertEquals(0L, VideoSeekRules.seekMs(0.5f, 0))
        assertEquals(2_500L, VideoSeekRules.seekMsAt(25f, 100f, 10_000))
        assertEquals(0L, VideoSeekRules.seekMsAt(10f, 0f, 10_000))
    }

    @Test
    fun fractionClockAndScrubberGate() {
        assertEquals(0f, VideoSeekRules.fraction(0, 10_000), 0.0001f)
        assertEquals(0.5f, VideoSeekRules.fraction(5_000, 10_000), 0.0001f)
        assertEquals(1f, VideoSeekRules.fraction(99_000, 10_000), 0.0001f)
        assertEquals(0f, VideoSeekRules.fraction(1_000, 0), 0.0001f)
        assertEquals(12_000L, VideoSeekRules.resolvedDuration(12_000, 8_000))
        assertEquals(8_000L, VideoSeekRules.resolvedDuration(0, 8_000))
        assertEquals(0L, VideoSeekRules.resolvedDuration(0, 0))
        assertEquals("0:05 / 0:10", VideoSeekRules.clock(5_000, 10_000))
        assertTrue(VideoSeekRules.showsScrubber(true, 1))
        assertFalse(VideoSeekRules.showsScrubber(true, 0))
        assertFalse(VideoSeekRules.showsScrubber(false, 10_000))
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
    }
}
