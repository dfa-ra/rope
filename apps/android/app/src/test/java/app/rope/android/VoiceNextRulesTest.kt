package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.VoiceNextRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class VoiceNextRulesTest {
    @Test
    fun localStoreStaysV6() {
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun nextIsLaterVoiceSkippingHoles() {
        val first = voice("a", 10L)
        val text = voice("t", 20L, kind = MessageKind.TEXT)
        val gone = voice("g", 30L, deleted = true)
        val note = voice("n", 40L, kind = MessageKind.VIDEO_NOTE)
        val missing = voice("m", 50L, path = null)
        val second = voice("b", 60L)
        val third = voice("c", 70L)
        val msgs = listOf(third, first, text, gone, note, missing, second)
        assertEquals("b", VoiceNextRules.next(msgs, "a")?.id)
        assertEquals("c", VoiceNextRules.next(msgs, "b")?.id)
        assertNull(VoiceNextRules.next(msgs, "c"))
        assertNull(VoiceNextRules.next(msgs, "missing-id"))
        assertFalse(VoiceNextRules.eligible(gone))
        assertFalse(VoiceNextRules.eligible(note))
        assertFalse(VoiceNextRules.eligible(missing))
        assertTrue(VoiceNextRules.eligible(first))
    }

    private fun voice(
        id: String,
        ts: Long,
        kind: MessageKind = MessageKind.VOICE,
        deleted: Boolean = false,
        path: String? = "/v.m4a",
    ) = ChatMessage(
        id = id,
        peerDeviceId = "peer",
        outgoing = false,
        text = "",
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = ts,
        kind = kind,
        localPath = path,
        deleted = deleted,
    )
}
