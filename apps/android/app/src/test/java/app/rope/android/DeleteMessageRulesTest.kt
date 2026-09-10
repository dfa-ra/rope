package app.rope.android

import app.rope.android.data.ChatActions
import app.rope.android.data.ChatMessage
import app.rope.android.data.DeleteMessageRules
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteMessageRulesTest {
    private fun msg(
        outgoing: Boolean,
        deleted: Boolean = false,
        kind: MessageKind = MessageKind.TEXT,
    ) = ChatMessage(
        id = "m1",
        peerDeviceId = "peer",
        outgoing = outgoing,
        text = "привет",
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        kind = kind,
        deleted = deleted,
    )

    @Test
    fun incomingIsLocalHideOnly() {
        val incoming = msg(outgoing = false)
        assertTrue(DeleteMessageRules.canShow(incoming))
        assertTrue(ChatActions.canDelete(incoming))
        assertEquals(DeleteMessageRules.FOR_ME, DeleteMessageRules.menuLabel(incoming))
        assertEquals("Удалить у себя", DeleteMessageRules.menuLabel(incoming))
        assertFalse(DeleteMessageRules.notifyPeer(outgoing = false, skipNetwork = false))
        assertFalse(DeleteMessageRules.notifyPeer(outgoing = false, skipNetwork = true))
    }

    @Test
    fun outgoingStillNotifiesPeer() {
        val mine = msg(outgoing = true)
        assertTrue(DeleteMessageRules.canShow(mine))
        assertEquals(DeleteMessageRules.LABEL, DeleteMessageRules.menuLabel(mine))
        assertEquals("Удалить", DeleteMessageRules.menuLabel(mine))
        assertTrue(DeleteMessageRules.notifyPeer(outgoing = true, skipNetwork = false))
        assertFalse(DeleteMessageRules.notifyPeer(outgoing = true, skipNetwork = true))
    }

    @Test
    fun deletedAndSavedSkip() {
        assertFalse(DeleteMessageRules.canShow(msg(outgoing = true, deleted = true)))
        assertFalse(DeleteMessageRules.canShow(msg(outgoing = false, deleted = true)))
        assertFalse(ChatActions.canDelete(msg(outgoing = false, deleted = true)))
        assertTrue(DeleteMessageRules.canShow(msg(outgoing = false, kind = MessageKind.IMAGE)))
        assertTrue(DeleteMessageRules.canShow(msg(outgoing = false, kind = MessageKind.CALL)))
    }
}
