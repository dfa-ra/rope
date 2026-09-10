package app.rope.android

import app.rope.android.data.ChatActions
import app.rope.android.data.ChatMessage
import app.rope.android.data.ChatPrefs
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.ProtectContentRules
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectContentRulesTest {
    @Test
    fun protectBlocksForwardAndCopy() {
        val msg = ChatMessage(
            id = "m1",
            peerDeviceId = "p",
            outgoing = false,
            text = "привет",
            status = MessageStatus.DELIVERED_TO_DEVICE,
            timestampMs = 1L,
            kind = MessageKind.TEXT,
        )
        assertTrue(ChatActions.canForward(msg))
        assertFalse(ChatActions.canForward(msg, protect = true))
        assertTrue(ChatActions.canCopy(msg))
        assertFalse(ChatActions.canCopy(msg, protect = true))
        assertTrue(ProtectContentRules.canForward(msg, protect = false))
        assertFalse(ProtectContentRules.canForward(msg, protect = true))
        assertTrue(ProtectContentRules.canCopy(msg, protect = false))
        assertFalse(ProtectContentRules.canCopy(msg, protect = true))
        assertFalse(ProtectContentRules.canForward(msg.copy(deleted = true), protect = false))
        assertFalse(ProtectContentRules.applies(SavedMessagesRules.ID))
        assertFalse(ProtectContentRules.applies(null))
        assertTrue(ProtectContentRules.applies("peer-1"))
        assertTrue(ProtectContentRules.applies("g:abc"))
        assertEquals("Запретить пересылку", ProtectContentRules.TITLE)
        assertTrue(ChatPrefs.parse("""{"protect":true}""").protect)
        assertFalse(ChatPrefs.parse("""{"protect":false}""").protect)
        assertFalse(ChatPrefs.parse(null).protect)
        assertEquals(6, LocalStore.VERSION)
    }
}
