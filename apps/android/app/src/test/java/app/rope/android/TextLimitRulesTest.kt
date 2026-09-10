package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.MediaSendRules
import app.rope.android.data.TextLimitRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextLimitRulesTest {
    @Test
    fun telegramCapIs4096AndCounterIsNOverMax() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals(4096, TextLimitRules.TEXT_MAX)
        assertEquals(1024, MediaSendRules.CAPTION_MAX)
        assertEquals("0 / 4096", TextLimitRules.counter(""))
        assertEquals("5 / 4096", TextLimitRules.counter("hello"))
        val over = "я".repeat(TextLimitRules.TEXT_MAX + 8)
        assertEquals("${TextLimitRules.TEXT_MAX} / 4096", TextLimitRules.counter(over))
        assertEquals("я".repeat(TextLimitRules.TEXT_MAX), TextLimitRules.limit(over))
        assertTrue(TextLimitRules.atLimit(over))
        assertFalse(TextLimitRules.atLimit("hi"))
        assertEquals("hi", TextLimitRules.limit("hi"))
    }

    @Test
    fun counterHiddenForCaptionsAndUntilNearCap() {
        val near = "a".repeat(TextLimitRules.TEXT_MAX - TextLimitRules.WARN_REMAINING)
        val below = "a".repeat(TextLimitRules.TEXT_MAX - TextLimitRules.WARN_REMAINING - 1)
        assertTrue(TextLimitRules.showCounter(near, pendingMedia = false))
        assertFalse(TextLimitRules.showCounter(below, pendingMedia = false))
        assertFalse(TextLimitRules.showCounter(near, pendingMedia = true))
        assertFalse(TextLimitRules.showCounter("hi", pendingMedia = false))
    }

    @Test
    fun composerUsesCaptionCapWhenMediaPending() {
        val huge = "б".repeat(MediaSendRules.CAPTION_MAX + 12)
        assertEquals(
            "б".repeat(MediaSendRules.CAPTION_MAX),
            TextLimitRules.forComposer(huge, pendingMedia = true),
        )
        assertEquals(
            "б".repeat(TextLimitRules.TEXT_MAX),
            TextLimitRules.forComposer("б".repeat(TextLimitRules.TEXT_MAX + 3), pendingMedia = false),
        )
        assertEquals("ок", TextLimitRules.forComposer("ок", pendingMedia = false))
        assertEquals("ок", TextLimitRules.forComposer("ок", pendingMedia = true))
    }
}
