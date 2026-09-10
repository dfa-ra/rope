package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.VoiceQualRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceQualRulesTest {
    @Test
    fun inheritStoreAndDefaults() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, BuildConfig.VERSION_CODE)
        assertEquals(VoiceQualRules.COMPRESSED, VoiceQualRules.normalize(null))
        assertEquals(VoiceQualRules.COMPRESSED, VoiceQualRules.normalize("  "))
        assertEquals(VoiceQualRules.COMPRESSED, VoiceQualRules.normalize("raw"))
        assertEquals(VoiceQualRules.HD, VoiceQualRules.normalize("hd"))
        assertEquals("Качество голоса", VoiceQualRules.TITLE)
        assertEquals(2, VoiceQualRules.OPTIONS.size)
    }

    @Test
    fun bitrateAndSample() {
        assertEquals(64_000, VoiceQualRules.bitrate(VoiceQualRules.COMPRESSED))
        assertEquals(96_000, VoiceQualRules.bitrate(VoiceQualRules.HD))
        assertEquals(44_100, VoiceQualRules.sampleRate(VoiceQualRules.COMPRESSED))
        assertEquals(48_000, VoiceQualRules.sampleRate(VoiceQualRules.HD))
        assertEquals(64_000, VoiceQualRules.bitrate("raw"))
        assertTrue(VoiceQualRules.hint().contains("HD"))
        assertFalse(VoiceQualRules.hint().contains('\n'))
    }
}
