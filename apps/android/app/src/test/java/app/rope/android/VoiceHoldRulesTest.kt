package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.VoiceHoldRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceHoldRulesTest {
    @Test
    fun inheritStoreAndVersion() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, BuildConfig.VERSION_CODE)
        assertEquals("2x", VoiceHoldRules.LABEL)
        assertFalse(VoiceHoldRules.LABEL.contains("FCM", ignoreCase = true))
    }

    @Test
    fun holdIs2xUntilRelease() {
        assertFalse(VoiceHoldRules.reachedHold(0L))
        assertFalse(VoiceHoldRules.reachedHold(VoiceHoldRules.HOLD_MS - 1L))
        assertTrue(VoiceHoldRules.reachedHold(VoiceHoldRules.HOLD_MS))
        assertEquals(1f, VoiceHoldRules.speed(holding = false), 0f)
        assertEquals(2f, VoiceHoldRules.speed(holding = true), 0f)
        assertEquals(1f, VoiceHoldRules.speed(holding = true, playing = false), 0f)
        assertEquals(1.5f, VoiceHoldRules.speed(holding = false, playing = true, base = 1.5f), 0f)
        assertEquals(2f, VoiceHoldRules.speed(holding = true, playing = true, base = 1.5f), 0f)
        assertTrue(VoiceHoldRules.chipVisible(holding = true))
        assertFalse(VoiceHoldRules.chipVisible(holding = false))
        assertFalse(VoiceHoldRules.chipVisible(holding = true, playing = false))
    }
}
