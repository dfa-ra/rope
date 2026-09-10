package app.rope.android

import app.rope.android.data.HideTypingRules
import app.rope.android.data.LocalStore
import app.rope.android.data.TypingRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HideTypingRulesTest {
    @Test
    fun kvDefaultsOff() {
        assertFalse(HideTypingRules.enabledFromKv(null))
        assertFalse(HideTypingRules.enabledFromKv("0"))
        assertFalse(HideTypingRules.enabledFromKv(""))
        assertTrue(HideTypingRules.enabledFromKv("1"))
        assertEquals("1", HideTypingRules.persist(true))
        assertEquals("0", HideTypingRules.persist(false))
    }

    @Test
    fun shouldSendSkipsWhenHidden() {
        assertTrue(TypingRules.shouldSend(0, 3_000, "hi"))
        assertTrue(TypingRules.shouldSend(0, 3_000, "hi", hideTyping = false))
        assertFalse(TypingRules.shouldSend(0, 3_000, "hi", hideTyping = true))
        assertFalse(TypingRules.shouldSend(0, 3_000, "  ", hideTyping = false))
        assertFalse(TypingRules.shouldSend(2_500, 3_000, "hi", hideTyping = false))
        assertTrue(HideTypingRules.hint().contains("набираете"))
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
    }
}
