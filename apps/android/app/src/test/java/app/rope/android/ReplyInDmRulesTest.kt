package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.Conversation
import app.rope.android.data.DirectoryDevice
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.ReplyInDmRules
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplyInDmRulesTest {
    @Test
    fun labelAndShowOnlyOnIncomingGroupLive() {
        assertEquals("Ответить в личке", ReplyInDmRules.LABEL)
        val incoming = groupMsg("m1", senderId = "bob", outgoing = false)
        assertTrue(ReplyInDmRules.canShow(isGroup = true, incoming, myDeviceId = "me"))
        assertFalse(ReplyInDmRules.canShow(isGroup = false, incoming, myDeviceId = "me"))
        assertFalse(ReplyInDmRules.canShow(isGroup = true, incoming.copy(outgoing = true), "me"))
        assertFalse(ReplyInDmRules.canShow(isGroup = true, incoming.copy(deleted = true), "me"))
        assertFalse(ReplyInDmRules.canShow(isGroup = true, incoming.copy(senderId = ""), "me"))
        assertFalse(ReplyInDmRules.canShow(isGroup = true, incoming.copy(senderId = "me"), "me"))
        assertFalse(ReplyInDmRules.canShow(isGroup = true, incoming.copy(senderId = "g:crew"), "me"))
        assertFalse(ReplyInDmRules.canShow(isGroup = true, incoming.copy(senderId = SavedMessagesRules.ID), "me"))
        assertEquals("bob", ReplyInDmRules.senderId(incoming))
        assertTrue(ReplyInDmRules.attachReply(incoming))
        assertFalse(ReplyInDmRules.attachReply(incoming.copy(deleted = true)))
    }

    @Test
    fun existingOneToOneWinsOverGroupAndSaved() {
        val bob = device("bob", "Боб")
        val dm = conv("bob", "Боб", isGroup = false, peer = bob)
        val leftover = conv("eve", "eve", isGroup = false, peer = null)
        val group = conv("g:crew", "Экипаж", isGroup = true)
        val saved = SavedMessagesRules.conversation(null, app.rope.android.data.ChatPrefs(), "me")
        val list = listOf(saved, group, leftover, dm)
        assertEquals("bob", ReplyInDmRules.existingDm(list, "bob")?.id)
        assertEquals("eve", ReplyInDmRules.existingDm(list, "eve")?.id)
        assertNull(ReplyInDmRules.existingDm(list, "g:crew"))
        assertNull(ReplyInDmRules.existingDm(list, SavedMessagesRules.ID))
        assertNull(ReplyInDmRules.existingDm(listOf(group, saved), "bob"))
        assertFalse(ReplyInDmRules.matchesSender(group, "bob"))
        assertFalse(ReplyInDmRules.matchesSender(saved, "bob"))
        assertTrue(ReplyInDmRules.matchesSender(dm, "BOB"))
        assertTrue(ReplyInDmRules.matchesSender(dm, "m-bob"))
    }

    @Test
    fun noExistingOpensStubOneToOneNotGroupId() {
        val msg = groupMsg("m2", senderId = "carol", senderName = "Карол")
        val peer = ReplyInDmRules.peerFor(msg, conversations = emptyList(), devices = emptyList())
        assertEquals("carol", peer?.deviceId)
        assertEquals("Карол", peer?.displayName)
        assertEquals("carol", ReplyInDmRules.chatId(null, peer!!))
        assertFalse(app.rope.android.data.ChatIds.isGroup(peer.deviceId))
        val listed = device("carol", "Карол")
        val fromDir = ReplyInDmRules.peerFor(msg, conversations = emptyList(), devices = listOf(listed))
        assertEquals("carol", fromDir?.deviceId)
        assertTrue(fromDir!!.publicIdentity.isNotEmpty())
        val existing = conv("carol", "Карол", isGroup = false, peer = device("carol", "Карол"))
        assertEquals("carol", ReplyInDmRules.chatId(existing, peer))
        assertNotEquals("g:crew", ReplyInDmRules.chatId(existing, peer))
    }

    @Test
    fun peerForPrefersExistingDirectoryDevice() {
        val bob = device("bob", "Боб")
        val dm = conv("bob", "Боб", isGroup = false, peer = bob)
        val msg = groupMsg("m3", senderId = "bob", senderName = "Боб")
        val other = device("carol", "Карол")
        val peer = ReplyInDmRules.peerFor(msg, listOf(dm), listOf(other, bob))
        assertEquals("bob", peer?.deviceId)
        assertEquals("Боб", peer?.displayName)
        val leftover = conv("eve", "Ева", isGroup = false, peer = null)
        val eveMsg = groupMsg("m4", senderId = "eve", senderName = "Ева")
        val stub = ReplyInDmRules.peerFor(eveMsg, listOf(leftover), devices = emptyList())
        assertEquals("eve", stub?.deviceId)
        assertEquals("Ева", stub?.displayName)
        assertTrue(stub?.publicIdentity?.isEmpty() == true)
    }

    @Test
    fun stubPeerFallsBackToShortIdAndKeepsStoreV6() {
        val stub = ReplyInDmRules.stubPeer("abcdef1234", "  ")
        assertEquals("abcdef12", stub.displayName)
        assertEquals("abcdef1234", stub.deviceId)
        assertEquals(6, LocalStore.VERSION)
    }

    private fun groupMsg(
        id: String,
        senderId: String,
        senderName: String = "Боб",
        outgoing: Boolean = false,
    ) = ChatMessage(
        id = id,
        peerDeviceId = "g:crew",
        outgoing = outgoing,
        text = "привет",
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1,
        kind = MessageKind.GROUP_TEXT,
        groupId = "crew",
        senderId = senderId,
        senderName = senderName,
    )

    private fun device(id: String, name: String) = DirectoryDevice(
        deviceId = id,
        memberId = "m-$id",
        displayName = name,
        publicIdentity = byteArrayOf(1, 2, 3),
        lastSeen = "",
        online = false,
    )

    private fun conv(
        id: String,
        title: String,
        isGroup: Boolean,
        peer: DirectoryDevice? = null,
    ) = Conversation(
        id = id,
        title = title,
        subtitle = "",
        isGroup = isGroup,
        online = false,
        last = null,
        peer = peer,
    )
}
