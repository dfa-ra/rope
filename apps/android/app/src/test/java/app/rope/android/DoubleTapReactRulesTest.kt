package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.DoubleTapReactRules
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.ReactionPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DoubleTapReactRulesTest {
    @Test
    fun doubleTapUsesFirstQuickReactionHeart() {
        assertEquals("❤️", DoubleTapReactRules.EMOJI)
        assertEquals(ReactionPayload.EMOJIS.first(), DoubleTapReactRules.EMOJI)
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun liveMessageReactsWhenNotSelecting() {
        val live = msg("a")
        assertTrue(DoubleTapReactRules.canReact(live, selecting = false))
        assertEquals("❤️", DoubleTapReactRules.emojiOrNull(live, selecting = false))
    }

    @Test
    fun deletedAndSelectingAreNoOps() {
        val live = msg("a")
        val gone = msg("b", deleted = true)
        assertFalse(DoubleTapReactRules.canReact(live, selecting = true))
        assertFalse(DoubleTapReactRules.canReact(gone, selecting = false))
        assertNull(DoubleTapReactRules.emojiOrNull(live, selecting = true))
        assertNull(DoubleTapReactRules.emojiOrNull(gone, selecting = false))
    }

    @Test
    fun outgoingAndIncomingBothReact() {
        val out = msg("out", outgoing = true)
        val inn = msg("in", outgoing = false)
        assertTrue(DoubleTapReactRules.canReact(out, selecting = false))
        assertTrue(DoubleTapReactRules.canReact(inn, selecting = false))
    }

    private fun msg(id: String, deleted: Boolean = false, outgoing: Boolean = false): ChatMessage =
        ChatMessage(
            id = id,
            peerDeviceId = "peer",
            outgoing = outgoing,
            text = "hi",
            status = MessageStatus.DELIVERED_TO_DEVICE,
            timestampMs = 1L,
            senderId = "them",
            kind = MessageKind.TEXT,
            deleted = deleted,
        )
}
