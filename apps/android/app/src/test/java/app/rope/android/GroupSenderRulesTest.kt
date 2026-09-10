package app.rope.android

import app.rope.android.data.ChatIds
import app.rope.android.data.ChatMessage
import app.rope.android.data.DirectoryDevice
import app.rope.android.data.GroupSenderRules
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageStatus
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupSenderRulesTest {
    @Test
    fun canOpenIncomingGroupSenderNotSelf() {
        val incoming = msg("peer-1", "Аня")
        assertTrue(GroupSenderRules.canOpen(true, incoming, "me"))
        assertTrue(GroupSenderRules.canOpen(true, msg("  PEER-1  ", "Аня"), "me"))
        assertTrue(GroupSenderRules.canOpen(true, incoming, null))
        assertFalse(GroupSenderRules.canOpen(false, incoming, "me"))
        assertFalse(GroupSenderRules.canOpen(true, incoming.copy(outgoing = true), "me"))
        assertFalse(GroupSenderRules.canOpen(true, msg("", "Аня"), "me"))
        assertFalse(GroupSenderRules.canOpen(true, msg("me", "Я"), "me"))
        assertFalse(GroupSenderRules.canOpen(true, msg("ME", "Я"), "me"))
        assertFalse(GroupSenderRules.canOpen(true, msg(SavedMessagesRules.ID, "Избранное"), "me"))
        assertFalse(GroupSenderRules.canOpen(true, msg(ChatIds.group("g-1"), "Группа"), "me"))
        assertEquals("Написать", GroupSenderRules.ACTION)
        assertEquals("Нет в справочнике", GroupSenderRules.NOTICE_MISSING)
        assertEquals("peer-1", GroupSenderRules.senderId(incoming))
        assertEquals("", GroupSenderRules.senderId(msg("null", "Аня")))
    }

    @Test
    fun peerPrefersDirectoryThenStubs() {
        val incoming = msg("peer-1", "Аня")
        val listed = person("peer-1", "Анна из справочника")
        assertEquals("Анна из справочника", GroupSenderRules.peerFor(incoming, listOf(listed))?.displayName)
        val stub = GroupSenderRules.peerFor(incoming, emptyList())
        assertEquals("peer-1", stub?.deviceId)
        assertEquals("Аня", stub?.displayName)
        assertEquals("peer-1", GroupSenderRules.stubPeer("peer-1", "  ").displayName)
        assertNull(GroupSenderRules.peerFor(msg("", "Аня"), listOf(listed)))
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
    }

    private fun msg(senderId: String, senderName: String) = ChatMessage(
        id = "m1",
        peerDeviceId = "g:crew",
        outgoing = false,
        text = "привет",
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        senderId = senderId,
        senderName = senderName,
    )

    private fun person(id: String, name: String): DirectoryDevice =
        DirectoryDevice(id, "m$id", name, ByteArray(0), "", online = false)
}
