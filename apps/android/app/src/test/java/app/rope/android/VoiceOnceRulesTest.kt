package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.MediaPayload
import app.rope.android.data.VoiceOnceRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceOnceRulesTest {
    @Test
    fun onceVoiceIncomingPlaysOnce() {
        assertTrue(VoiceOnceRules.canMark("voice"))
        assertFalse(VoiceOnceRules.canMark("image"))
        assertTrue(VoiceOnceRules.pack("voice", true))
        assertFalse(VoiceOnceRules.pack("file", true))

        val bare = MediaPayload("voice", "o", "ab", "KEY", "audio/mp4", "v.m4a", 1, 1200)
        assertFalse(MediaPayload.parse(bare.toJson()).once)
        assertFalse(bare.toJson().contains("once"))
        val marked = bare.copy(once = true)
        assertTrue(MediaPayload.parse(marked.toJson()).once)
        assertTrue(marked.toJson().contains("\"once\":true"))

        assertTrue(VoiceOnceRules.canPlay(outgoing = true, flagged = true, heard = true))
        assertTrue(VoiceOnceRules.canPlay(outgoing = false, flagged = true, heard = false))
        assertFalse(VoiceOnceRules.canPlay(outgoing = false, flagged = true, heard = true))
        assertTrue(VoiceOnceRules.canPlay(outgoing = false, flagged = false, heard = true))
        assertFalse(VoiceOnceRules.canSeek(true))
        assertTrue(VoiceOnceRules.canSeek(false))
        assertTrue(VoiceOnceRules.consumeOnComplete(outgoing = false, flagged = true))
        assertFalse(VoiceOnceRules.consumeOnComplete(outgoing = true, flagged = true))
        assertTrue(VoiceOnceRules.skipDownload(outgoing = false, flagged = true, heard = true))
        assertEquals("Один раз", VoiceOnceRules.LABEL)
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun heardKvRejectsControlChars() {
        val marked = VoiceOnceRules.mark(emptySet(), "m1")
        assertTrue("m1" in marked)
        assertEquals(setOf("m1"), VoiceOnceRules.heardIds(VoiceOnceRules.putHeard(marked)))
        assertEquals(emptySet<String>(), VoiceOnceRules.heardIds(null))
        assertEquals(null, VoiceOnceRules.sanitizeId("a\nb"))
        assertEquals(marked, VoiceOnceRules.mark(marked, "m1\n"))
    }
}
