package app.rope.android

import app.rope.android.data.ChatControlRules
import app.rope.android.data.ChatIds
import app.rope.android.data.ChatMessage
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatControlRulesTest {
    private val alice = "a".repeat(64)
    private val bob = "b".repeat(64)

    private fun msg(
        sender: String,
        peer: String = bob,
        groupId: String? = null,
        deleted: Boolean = false,
    ) = ChatMessage(
        id = "m1",
        peerDeviceId = if (groupId != null) ChatIds.group(groupId) else peer,
        outgoing = false,
        text = "hi",
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        kind = if (groupId != null) MessageKind.GROUP_TEXT else MessageKind.TEXT,
        groupId = groupId,
        senderId = sender,
        deleted = deleted,
    )

    @Test
    fun authorCanEditAndDeleteOwnMessage() {
        val own = msg(alice, peer = bob)
        assertTrue(ChatControlRules.allowEdit(alice, own))
        assertTrue(ChatControlRules.allowDelete(alice, own))
        assertFalse(ChatControlRules.allowEdit(bob, own))
        assertFalse(ChatControlRules.allowDelete(bob, own))
        assertFalse(ChatControlRules.allowDelete(alice, null))
        assertFalse(ChatControlRules.allowEdit("", own))
    }

    @Test
    fun pinStaysInTheThreadTheActorShares() {
        val dm = msg(alice, peer = bob)
        assertTrue(ChatControlRules.allowPin(bob, dm, null))
        assertFalse(ChatControlRules.allowPin(alice, dm, null))
        val gid = "11111111-2222-3333-4444-555555555555"
        val groupMsg = msg(alice, groupId = gid)
        assertTrue(ChatControlRules.allowPin(bob, groupMsg, listOf(alice, bob)))
        assertFalse(ChatControlRules.allowPin(bob, groupMsg, listOf(alice)))
        assertFalse(ChatControlRules.allowPin(bob, groupMsg, null))
        assertFalse(ChatControlRules.allowPin(bob, dm.copy(deleted = true), null))
    }

    @Test
    fun reactStaysInTheThreadTheActorShares() {
        val dm = msg(alice, peer = bob)
        assertTrue(ChatControlRules.allowReact(bob, dm, null))
        assertFalse(ChatControlRules.allowReact(alice, dm, null))
        val gid = "11111111-2222-3333-4444-555555555555"
        val groupMsg = msg(alice, groupId = gid)
        assertTrue(ChatControlRules.allowReact(bob, groupMsg, listOf(alice, bob)))
        assertFalse(ChatControlRules.allowReact(bob, groupMsg, listOf(alice)))
        assertFalse(ChatControlRules.allowReact(bob, groupMsg, null))
        assertFalse(ChatControlRules.allowReact(bob, dm.copy(deleted = true), null))
        assertFalse(ChatControlRules.allowReact(bob, null, listOf(bob)))
    }

    @Test
    fun typingIgnoresUnknownGroups() {
        val gid = "11111111-2222-3333-4444-555555555555"
        assertEquals(alice, ChatControlRules.typingChatId(alice, null, emptyList()))
        assertEquals(alice, ChatControlRules.typingChatId(alice, bob, listOf(gid)))
        assertEquals(ChatIds.group(gid), ChatControlRules.typingChatId(alice, ChatIds.group(gid), listOf(gid)))
        assertNull(ChatControlRules.typingChatId(alice, ChatIds.group(gid), emptyList()))
        assertNull(ChatControlRules.typingChatId("", bob, emptyList()))
    }

    @Test
    fun chosenMessageIdDoesNotClobberAnotherSender() {
        assertTrue(ChatControlRules.shouldReplace(alice, alice))
        assertTrue(ChatControlRules.shouldReplace(alice.uppercase(), alice))
        assertFalse(ChatControlRules.shouldReplace(alice, bob))
        assertTrue(ChatControlRules.shouldReplace("", bob))
        assertTrue(ChatControlRules.shouldReplace(alice, null))
        assertTrue(ChatControlRules.shouldReplace(null, null))
    }
}
