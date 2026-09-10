package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.SentSoundRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SentSoundRulesTest {
    @Test
    fun localStoreStaysV6() {
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun kvDefaultsOnAndMuteWins() {
        assertTrue(SentSoundRules.enabledFromKv(null))
        assertTrue(SentSoundRules.enabledFromKv("1"))
        assertFalse(SentSoundRules.enabledFromKv("0"))
        assertTrue(SentSoundRules.shouldPlay(enabled = true, globalMuted = false))
        assertFalse(SentSoundRules.shouldPlay(enabled = true, globalMuted = true))
        assertFalse(SentSoundRules.shouldPlay(enabled = false, globalMuted = false))
        assertEquals("Звук отправки", SentSoundRules.TITLE)
    }
}
