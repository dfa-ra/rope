package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.VideoHoldRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoHoldRulesTest {
    @Test
    fun localStoreStaysV6() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, BuildConfig.VERSION_CODE)
    }

    @Test
    fun holdIs2xUntilRelease() {
        assertFalse(VideoHoldRules.reachedHold(0L))
        assertFalse(VideoHoldRules.reachedHold(VideoHoldRules.HOLD_MS - 1L))
        assertTrue(VideoHoldRules.reachedHold(VideoHoldRules.HOLD_MS))
        assertEquals(1f, VideoHoldRules.speed(holding = false), 0f)
        assertEquals(2f, VideoHoldRules.speed(holding = true), 0f)
        assertEquals(1f, VideoHoldRules.speed(holding = true, playing = false), 0f)
        assertTrue(VideoHoldRules.chipVisible(holding = true))
        assertFalse(VideoHoldRules.chipVisible(holding = false))
        assertFalse(VideoHoldRules.chipVisible(holding = true, playing = false))
        assertEquals("2x", VideoHoldRules.LABEL)
    }
}
