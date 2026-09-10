package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.VoiceSpeakerRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceSpeakerRulesTest {
    @Test
    fun inheritStoreAndToggle() {
        assertEquals(6, LocalStore.VERSION)
        assertTrue(VoiceSpeakerRules.DEFAULT_SPEAKER)
        assertFalse(VoiceSpeakerRules.toggle(true))
        assertTrue(VoiceSpeakerRules.toggle(false))
        assertEquals("динамик", VoiceSpeakerRules.label(true))
        assertEquals("трубка", VoiceSpeakerRules.label(false))
        assertEquals("Играть в трубку", VoiceSpeakerRules.contentDescription(true))
        assertEquals("Играть с динамика", VoiceSpeakerRules.contentDescription(false))
        assertFalse(VoiceSpeakerRules.label(true).contains("FCM", ignoreCase = true))
    }
}
