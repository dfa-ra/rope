package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.LocalStore
import app.rope.android.data.MediaPayload
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.ViewOnceRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewOnceRulesTest {
    @Test
    fun incomingPhotoOpensOnce() {
        assertTrue(ViewOnceRules.canMark("image"))
        assertTrue(ViewOnceRules.canMark(MessageKind.VIDEO))
        assertFalse(ViewOnceRules.canMark("voice"))
        assertTrue(ViewOnceRules.pack("video", true))
        assertFalse(ViewOnceRules.pack("voice", true))

        val bare = MediaPayload("image", "o", "ab", "KEY", "image/jpeg", "p.jpg", 1)
        assertFalse(MediaPayload.parse(bare.toJson()).once)
        assertFalse(bare.toJson().contains("once"))
        val marked = bare.copy(once = true)
        assertTrue(MediaPayload.parse(marked.toJson()).once)
        assertTrue(marked.toJson().contains("\"once\":true"))
        assertEquals("Фото · один просмотр", marked.preview())

        val msg = ChatMessage(
            id = "m1",
            peerDeviceId = "p",
            outgoing = false,
            text = "Фото",
            status = MessageStatus.DELIVERED_TO_DEVICE,
            timestampMs = 1L,
            kind = MessageKind.IMAGE,
            extra = marked.toJson(),
        )
        assertTrue(ViewOnceRules.canOpen(outgoing = false, flagged = true, viewed = false))
        assertFalse(ViewOnceRules.canOpen(outgoing = false, flagged = true, viewed = true))
        assertTrue(ViewOnceRules.canOpen(outgoing = true, flagged = true, viewed = true))
        assertTrue(ViewOnceRules.consumeOnOpen(outgoing = false, flagged = true))
        assertFalse(ViewOnceRules.consumeOnOpen(outgoing = true, flagged = true))
        assertTrue(ViewOnceRules.locked(msg, emptySet()))
        assertFalse(ViewOnceRules.placeholder(msg, emptySet()))
        assertFalse(ViewOnceRules.locked(msg, setOf("m1")))
        assertTrue(ViewOnceRules.placeholder(msg, setOf("m1")))
        assertFalse(ViewOnceRules.placeholder(msg.copy(outgoing = true), setOf("m1")))
        assertFalse(ViewOnceRules.locked(msg.copy(outgoing = true), emptySet()))
        assertTrue(ViewOnceRules.skipDownload(outgoing = false, flagged = true, viewed = true))
        assertFalse(ViewOnceRules.skipDownload(outgoing = false, flagged = true, viewed = false))
        assertFalse(ViewOnceRules.skipDownload(outgoing = true, flagged = true, viewed = true))
        assertTrue(ViewOnceRules.viewerAlone(msg))
        assertEquals("Один просмотр", ViewOnceRules.LABEL)
        assertEquals("Просмотрено", ViewOnceRules.VIEWED)
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun pendingChipAndKv() {
        assertTrue(ViewOnceRules.pendingEligible(listOf("image/jpeg")))
        assertTrue(ViewOnceRules.pendingEligible(listOf("video/mp4"), listOf("c.mp4")))
        assertFalse(ViewOnceRules.pendingEligible(listOf("application/pdf"), listOf("a.pdf")))
        val marked = ViewOnceRules.mark(emptySet(), "m1")
        assertEquals(setOf("m1"), ViewOnceRules.viewedIds(ViewOnceRules.putViewed(marked)))
        assertEquals(null, ViewOnceRules.sanitizeId("a\nb"))
    }
}
