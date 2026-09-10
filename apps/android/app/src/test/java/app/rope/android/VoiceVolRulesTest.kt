package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.VoiceVolRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceVolRulesTest {
    @Test
    fun inheritStoreAndDefaultFull() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals(VoiceVolRules.VOL_100, VoiceVolRules.parse(null))
        assertEquals(VoiceVolRules.VOL_100, VoiceVolRules.parse(""))
        assertEquals(VoiceVolRules.VOL_100, VoiceVolRules.parse("nope"))
        assertEquals(VoiceVolRules.VOL_100, VoiceVolRules.parse("25\nbad"))
        assertEquals(VoiceVolRules.VOL_100, VoiceVolRules.parse("50\r"))
        assertEquals(VoiceVolRules.VOL_100, VoiceVolRules.parse("100\u0000"))
        assertEquals(VoiceVolRules.VOL_25, VoiceVolRules.parse(" 25 "))
        assertEquals(VoiceVolRules.VOL_50, VoiceVolRules.parse("50"))
        assertEquals(VoiceVolRules.VOL_100, VoiceVolRules.parse("100"))
        assertFalse(VoiceVolRules.KEY.contains("FCM", ignoreCase = true))
    }

    @Test
    fun cycleAndLabels() {
        assertEquals(VoiceVolRules.VOL_50, VoiceVolRules.next(VoiceVolRules.VOL_25))
        assertEquals(VoiceVolRules.VOL_100, VoiceVolRules.next(VoiceVolRules.VOL_50))
        assertEquals(VoiceVolRules.VOL_25, VoiceVolRules.next(VoiceVolRules.VOL_100))
        assertEquals("25%", VoiceVolRules.label(0.2f))
        assertEquals("50%", VoiceVolRules.label(0.5f))
        assertEquals("100%", VoiceVolRules.label(0.9f))
        assertEquals("25", VoiceVolRules.stored(VoiceVolRules.VOL_25))
        assertEquals("50", VoiceVolRules.stored(VoiceVolRules.VOL_50))
        assertEquals("100", VoiceVolRules.stored(VoiceVolRules.VOL_100))
        assertEquals(VoiceVolRules.VOL_25, VoiceVolRules.clamp(0.1f))
        assertEquals(VoiceVolRules.VOL_50, VoiceVolRules.clamp(0.5f))
        assertEquals(VoiceVolRules.VOL_100, VoiceVolRules.clamp(1.2f))
        assertEquals(3, VoiceVolRules.LEVELS.size)
    }
}
