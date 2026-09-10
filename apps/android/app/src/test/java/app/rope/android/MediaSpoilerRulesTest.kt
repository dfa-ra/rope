package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.LocalStore
import app.rope.android.data.MediaPayload
import app.rope.android.data.MediaSpoilerRules
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaSpoilerRulesTest {
    @Test
    fun extraJsonRoundTripAndHiddenUntilReveal() {
        assertTrue(MediaSpoilerRules.canMark("image"))
        assertTrue(MediaSpoilerRules.canMark(MessageKind.VIDEO))
        assertFalse(MediaSpoilerRules.canMark("voice"))
        assertFalse(MediaSpoilerRules.canMark(MessageKind.FILE))
        assertTrue(MediaSpoilerRules.pack("image", true))
        assertFalse(MediaSpoilerRules.pack("voice", true))
        assertFalse(MediaSpoilerRules.pack("image", false))

        val bare = MediaPayload("image", "o", "ab", "KEY", "image/jpeg", "p.jpg", 1)
        assertFalse(MediaPayload.parse(bare.toJson()).spoiler)
        assertFalse(bare.toJson().contains("spoiler"))

        val marked = bare.copy(spoiler = true)
        val got = MediaPayload.parse(marked.toJson())
        assertTrue(got.spoiler)
        assertTrue(marked.toJson().contains("\"spoiler\":true"))

        val msg = ChatMessage(
            id = "m1",
            peerDeviceId = "p",
            outgoing = true,
            text = "Фото",
            status = MessageStatus.CREATED,
            timestampMs = 1L,
            kind = MessageKind.IMAGE,
            extra = marked.toJson(),
        )
        assertTrue(MediaSpoilerRules.hidden(msg, emptySet()))
        assertFalse(MediaSpoilerRules.hidden(msg, MediaSpoilerRules.reveal(emptySet(), "m1")))
        assertFalse(MediaSpoilerRules.hidden(msg.copy(deleted = true), emptySet()))
        assertEquals("Скрыть", MediaSpoilerRules.LABEL)
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun pendingChipOnlyForPhotosAndVideos() {
        assertTrue(MediaSpoilerRules.pendingEligible(listOf("image/jpeg")))
        assertTrue(MediaSpoilerRules.pendingEligible(listOf("video/mp4"), listOf("c.mp4")))
        assertFalse(MediaSpoilerRules.pendingEligible(listOf("application/pdf"), listOf("a.pdf")))
        assertFalse(MediaSpoilerRules.pendingEligible(emptyList()))
    }
}
