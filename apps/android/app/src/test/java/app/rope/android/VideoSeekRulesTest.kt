package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.VideoSeekRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoSeekRulesTest {
    @Test
    fun mapsFractionAndPointerOntoDuration() {
        assertEquals(0L, VideoSeekRules.fromProgress(-1f, 10_000))
        assertEquals(5_000L, VideoSeekRules.fromProgress(0.5f, 10_000))
        assertEquals(10_000L, VideoSeekRules.fromProgress(2f, 10_000))
        assertEquals(0L, VideoSeekRules.fromProgress(0.5f, 0))
        assertEquals(2_500L, VideoSeekRules.fromPointerX(25f, 100f, 10_000))
        assertEquals(0L, VideoSeekRules.fromPointerX(10f, 0f, 10_000))
        assertEquals(0.5f, VideoSeekRules.progress(5_000, 10_000), 0.0001f)
        assertEquals(0f, VideoSeekRules.progress(5_000, 0), 0.0001f)
        assertEquals(10_000L, VideoSeekRules.clamp(99_000, 10_000))
        assertTrue(VideoSeekRules.canScrub(true, 1_000))
        assertFalse(VideoSeekRules.canScrub(true, 0))
        assertFalse(VideoSeekRules.canScrub(false, 10_000))
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
    }
}
