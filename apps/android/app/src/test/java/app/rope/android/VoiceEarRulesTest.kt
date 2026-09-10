package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.VoiceEarRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceEarRulesTest {
    @Test
    fun localStoreStaysV6() {
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun kvDefaultsSpeakerAndCallWins() {
        assertFalse(VoiceEarRules.earpieceFromKv(null))
        assertFalse(VoiceEarRules.earpieceFromKv("0"))
        assertTrue(VoiceEarRules.earpieceFromKv("1"))
        assertTrue(VoiceEarRules.useEarpiece(enabled = true, liveCall = false))
        assertFalse(VoiceEarRules.useEarpiece(enabled = true, liveCall = true))
        assertFalse(VoiceEarRules.useEarpiece(enabled = false, liveCall = false))
        assertEquals("Наушник для голосовых", VoiceEarRules.TITLE)
    }
}
