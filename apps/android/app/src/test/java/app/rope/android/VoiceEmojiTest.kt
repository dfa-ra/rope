package app.rope.android

import app.rope.android.data.EmojiPack
import app.rope.android.data.MessageStatus
import app.rope.android.data.ReactionPayload
import app.rope.android.data.VoicePlayback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceEmojiTest {
    @Test
    fun progressFractionIsPositionOverDuration() {
        assertEquals(0f, VoicePlayback.fraction(0, 12_000), 0.0001f)
        assertEquals(0.25f, VoicePlayback.fraction(3_000, 12_000), 0.0001f)
        assertEquals(0.5f, VoicePlayback.fraction(6_000, 12_000), 0.0001f)
        assertEquals(1f, VoicePlayback.fraction(12_000, 12_000), 0.0001f)
        assertEquals(1f, VoicePlayback.fraction(20_000, 12_000), 0.0001f)
        assertEquals(0f, VoicePlayback.fraction(1_000, 0), 0.0001f)
        assertEquals(0f, VoicePlayback.fraction(-5, -1), 0.0001f)
    }

    @Test
    fun progressClockAndResolvedDuration() {
        assertEquals("0:03 / 0:12", VoicePlayback.clock(3_400, 12_000))
        assertEquals("0:00 / 0:00", VoicePlayback.clock(0, 0))
        assertEquals(4_000L, VoicePlayback.resolvedDuration(4_000, 12_000))
        assertEquals(12_000L, VoicePlayback.resolvedDuration(0, 12_000))
        assertEquals(0L, VoicePlayback.resolvedDuration(0, 0))
        assertEquals(0L, VoicePlayback.displayPosition(false, 3_000))
        assertEquals(3_000L, VoicePlayback.displayPosition(true, 3_000))
    }

    @Test
    fun outgoingCreatedVoiceIsSending() {
        assertTrue(VoicePlayback.isSending(true, MessageStatus.CREATED))
        assertFalse(VoicePlayback.isSending(true, MessageStatus.SENT_TO_SERVER))
        assertFalse(VoicePlayback.isSending(true, MessageStatus.DELIVERED_TO_DEVICE))
        assertFalse(VoicePlayback.isSending(false, MessageStatus.CREATED))
    }

    @Test
    fun waveformBarsStayInUnitRange() {
        val bars = VoicePlayback.waveform("msg-1")
        assertEquals(VoicePlayback.BARS, bars.size)
        assertTrue(bars.all { it in 0.28f..1f })
        assertEquals(VoicePlayback.waveform("msg-1"), VoicePlayback.waveform("msg-1"))
        assertEquals(1.5f, VoicePlayback.nextSpeed(1f), 0.0001f)
        assertEquals("2x", VoicePlayback.speedLabel(2f))
    }

    @Test
    fun emojiPackHasCategoriesAndHundredsOfEmoji() {
        assertTrue(EmojiPack.categories.size >= 6)
        assertTrue(EmojiPack.all.size >= 300)
        assertEquals(EmojiPack.all.size, EmojiPack.all.distinct().size)
        EmojiPack.categories.forEach { cat ->
            assertTrue("${cat.id} is empty", cat.emojis.isNotEmpty())
            assertTrue(cat.label.isNotBlank())
            assertTrue(cat.icon.isNotBlank())
        }
        assertTrue(EmojiPack.smileys.contains("😂"))
        assertTrue(EmojiPack.gestures.contains("👍"))
        assertTrue(EmojiPack.hearts.contains("❤️"))
        assertEquals(ReactionPayload.EMOJIS, EmojiPack.quickReactions)
        assertEquals(
            listOf("❤️", "👌", "🤯", "😃", "👍", "😇", "😢"),
            ReactionPayload.EMOJIS,
        )
    }

    @Test
    fun emojiSearchFindsCategories() {
        assertTrue(EmojiPack.search("еда").contains("🍕"))
        assertTrue(EmojiPack.search("улыбки").contains("😀"))
        assertTrue(EmojiPack.search("сердца").contains("❤️"))
        assertEquals(EmojiPack.all, EmojiPack.search("   "))
        assertTrue(EmojiPack.search("🍕").contains("🍕"))
        assertTrue(EmojiPack.search("неттакогоxyz").isEmpty())
    }
}
