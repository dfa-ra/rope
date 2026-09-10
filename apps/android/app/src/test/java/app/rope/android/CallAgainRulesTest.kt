package app.rope.android

import app.rope.android.data.CallAgainRules
import app.rope.android.data.ChatMessage
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.VideoCallRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallAgainRulesTest {
    private fun call(text: String, deleted: Boolean = false) = ChatMessage(
        id = "1",
        peerDeviceId = "p",
        outgoing = false,
        text = text,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        kind = MessageKind.CALL,
        deleted = deleted,
    )

    @Test
    fun redialOnLiveDmCallNotGroupOrSaved() {
        val incoming = call(VideoCallRules.recordLabel(false, outgoing = false))
        assertTrue(CallAgainRules.canRedial(incoming, isGroup = false, saved = false))
        assertFalse(CallAgainRules.canRedial(incoming, isGroup = true, saved = false))
        assertFalse(CallAgainRules.canRedial(incoming, isGroup = false, saved = true))
        assertFalse(CallAgainRules.canRedial(incoming.copy(deleted = true), isGroup = false, saved = false))
        assertFalse(
            CallAgainRules.canRedial(
                incoming.copy(kind = MessageKind.TEXT),
                isGroup = false,
                saved = false,
            ),
        )
    }

    @Test
    fun videoLabelPicksVideoCall() {
        assertFalse(CallAgainRules.usesVideo("Входящий звонок"))
        assertTrue(CallAgainRules.usesVideo("Входящий видеозвонок"))
        assertTrue(CallAgainRules.usesVideo(VideoCallRules.recordLabel(true, outgoing = true)))
        assertEquals("Позвонить", CallAgainRules.a11y("Исходящий звонок"))
        assertEquals("Видеозвонок", CallAgainRules.a11y("Исходящий видеозвонок"))
        assertEquals(6, LocalStore.VERSION)
    }
}
