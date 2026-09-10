package app.rope.android

import app.rope.android.data.ChatListHit
import app.rope.android.data.ChatListPreviewKind
import app.rope.android.data.ChatListPreviewRules
import app.rope.android.data.ChatListRules
import app.rope.android.data.ChatMessage
import app.rope.android.data.Conversation
import app.rope.android.data.GroupChatUx
import app.rope.android.data.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatListPreviewRulesTest {
    private fun msg(
        text: String,
        outgoing: Boolean,
        senderId: String = "",
        senderName: String = "",
    ) = ChatMessage(
        id = "m",
        peerDeviceId = "peer",
        outgoing = outgoing,
        text = text,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1_000L,
        senderId = senderId,
        senderName = senderName,
    )

    @Test
    fun draftBeatsLastMessage() {
        val last = msg("привет", outgoing = false, senderName = "Аня")
        val copy = ChatListPreviewRules.copy(
            last = last,
            draft = "  черновик текст  ",
            isGroup = false,
            myDeviceId = "me",
            online = true,
        )
        assertEquals(ChatListPreviewKind.DRAFT, copy.kind)
        assertEquals("Черновик: черновик текст", copy.text)
        assertTrue(ChatListPreviewRules.isDraft(copy.text))
    }

    @Test
    fun whitespaceDraftIsIgnored() {
        val last = msg("ок", outgoing = true, senderId = "me")
        val copy = ChatListPreviewRules.copy(
            last = last,
            draft = " \t ",
            isGroup = false,
            myDeviceId = "me",
        )
        assertEquals(ChatListPreviewKind.LAST, copy.kind)
        assertEquals("Вы: ок", copy.text)
        assertFalse(ChatListPreviewRules.isDraft(copy.text))
    }

    @Test
    fun dmOutgoingUsesYouPrefix() {
        val last = msg("фото", outgoing = true, senderId = "me")
        val copy = ChatListPreviewRules.copy(last, draft = "", isGroup = false, myDeviceId = "me")
        assertEquals("Вы: фото", copy.text)
        assertEquals(ChatListPreviewKind.LAST, copy.kind)
        assertEquals(GroupChatUx.YOU, ChatListPreviewRules.YOU)
    }

    @Test
    fun dmIncomingHasNoYouPrefix() {
        val last = msg("привет", outgoing = false, senderId = "d2", senderName = "Аня")
        val copy = ChatListPreviewRules.copy(last, draft = "", isGroup = false, myDeviceId = "me")
        assertEquals("привет", copy.text)
        assertFalse(copy.text.startsWith("Вы:"))
    }

    @Test
    fun groupKeepsSenderColonPreview() {
        val last = msg("привет", outgoing = false, senderId = "d2", senderName = "Аня")
        val copy = ChatListPreviewRules.copy(
            last,
            draft = "",
            isGroup = true,
            myDeviceId = "me",
            memberCount = 3,
        )
        assertEquals("Аня: привет", copy.text)
        assertEquals(ChatListPreviewKind.LAST, copy.kind)
    }

    @Test
    fun groupOutgoingUsesYou() {
        val last = msg("ок", outgoing = true, senderId = "me", senderName = "Боря")
        val copy = ChatListPreviewRules.copy(last, draft = "", isGroup = true, myDeviceId = "me")
        assertEquals("Вы: ок", copy.text)
    }

    @Test
    fun idleDmUsesPresenceNotLast() {
        val online = ChatListPreviewRules.copy(null, "", isGroup = false, online = true)
        assertEquals(ChatListPreviewKind.PRESENCE, online.kind)
        assertEquals("в сети", online.text)
        val offline = ChatListPreviewRules.copy(null, "", isGroup = false, online = false)
        assertEquals("не в сети", offline.text)
    }

    @Test
    fun idleGroupUsesMemberCount() {
        val copy = ChatListPreviewRules.copy(null, "", isGroup = true, memberCount = 4)
        assertEquals("4 участников", copy.text)
        assertEquals(ChatListPreviewKind.PRESENCE, copy.kind)
    }

    @Test
    fun longDraftIsClippedToOneLine() {
        val body = ChatListPreviewRules.clip("я".repeat(90) + "\nдва")
        assertTrue(body.length <= ChatListPreviewRules.BODY_MAX)
        assertFalse(body.contains('\n'))
        assertFalse(ChatListPreviewRules.isDraft(body))
        val copy = ChatListPreviewRules.copy(null, "я".repeat(90), isGroup = false)
        assertEquals(ChatListPreviewKind.DRAFT, copy.kind)
        assertTrue(copy.text.startsWith("Черновик: "))
        assertTrue(copy.text.length <= "Черновик: ".length + ChatListPreviewRules.BODY_MAX)
    }

    @Test
    fun lastMessageNewlinesAreOneLine() {
        val last = msg("первая\nвторая\r\nтретья", outgoing = false, senderName = "Аня")
        val dm = ChatListPreviewRules.copy(last, draft = "", isGroup = false, myDeviceId = "me")
        assertEquals("первая вторая третья", dm.text)
        assertFalse(dm.text.contains('\n'))
        assertFalse(dm.text.contains('\r'))
        val group = ChatListPreviewRules.copy(
            last,
            draft = "",
            isGroup = true,
            myDeviceId = "me",
        )
        assertEquals("Аня: первая вторая третья", group.text)
        assertFalse(group.text.contains('\n'))
        val crlfName = msg("привет", outgoing = false, senderName = "Аня\nAdmin")
        val named = ChatListPreviewRules.copy(
            crlfName,
            draft = "",
            isGroup = true,
            myDeviceId = "me",
        )
        assertFalse(named.text.contains('\n'))
        assertEquals("Аня Admin: привет", named.text)
    }

    @Test
    fun draftWithoutLastIsSearchablePreview() {
        val draft = Conversation("1", "Анна", "Черновик: секрет", false, true, last = null)
        assertEquals(ChatListHit.PREVIEW, ChatListRules.hit(draft, "секрет"))
        assertTrue(ChatListRules.matches(draft, "секрет"))
        val presence = Conversation("2", "Кира", "в сети", false, true, last = null)
        assertEquals(ChatListHit.NONE, ChatListRules.hit(presence, "сети"))
    }
}
