package app.rope.android

import app.rope.android.data.ChatIds
import app.rope.android.data.ChatListHit
import app.rope.android.data.ChatListMode
import app.rope.android.data.ChatListPreviewKind
import app.rope.android.data.ChatListPreviewRules
import app.rope.android.data.ChatListRules
import app.rope.android.data.ChatMessage
import app.rope.android.data.ChatPrefs
import app.rope.android.data.ChatRouting
import app.rope.android.data.Conversation
import app.rope.android.data.ForwardRules
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.PeerProfileRules
import app.rope.android.data.SavedMessagesRules
import app.rope.android.data.ThreadEmptyRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedMessagesRulesTest {
    @Test
    fun idAndTitleAreLocalSavedMessages() {
        assertEquals("saved:", SavedMessagesRules.ID)
        assertEquals("Избранное", SavedMessagesRules.TITLE)
        assertTrue(SavedMessagesRules.isSaved(SavedMessagesRules.ID))
        assertTrue(ChatIds.isSaved(SavedMessagesRules.ID))
        assertFalse(SavedMessagesRules.isSaved("peer"))
        assertFalse(ChatIds.isSaved("g:gid"))
        assertFalse(ChatIds.isGroup(SavedMessagesRules.ID))
        assertFalse(ChatRouting.showLeftoverThread(SavedMessagesRules.ID))
        assertTrue(ChatRouting.showLeftoverThread("peer-1"))
    }

    @Test
    fun alwaysOnChatsTabNeverGroupsOrCallsAndDefaultsPinned() {
        assertTrue(SavedMessagesRules.visible(ChatListMode.ALL))
        assertFalse(SavedMessagesRules.visible(ChatListMode.GROUPS))
        assertFalse(SavedMessagesRules.visible(ChatListMode.CALLS))
        assertTrue(SavedMessagesRules.defaultPrefs(null).pinned)
        assertFalse(SavedMessagesRules.defaultPrefs(ChatPrefs(pinned = false)).pinned)
        val conv = SavedMessagesRules.conversation(last = null, prefs = ChatPrefs(pinned = true), myDeviceId = "me")
        assertTrue(SavedMessagesRules.isSaved(conv))
        assertEquals("Избранное", conv.title)
        assertTrue(conv.pinned)
        assertEquals(0, conv.unread)
        assertFalse(conv.isGroup)
        assertFalse(conv.online)
        assertEquals(SavedMessagesRules.ID, conv.peer?.deviceId)
        assertTrue(conv.peer?.publicIdentity?.isEmpty() == true)
        assertTrue(ChatListRules.matches(conv, "", ChatListMode.ALL))
        assertFalse(ChatListRules.matches(conv, "", ChatListMode.GROUPS))
        assertEquals(ChatListHit.TITLE_PREFIX, ChatListRules.hit(conv, "избр"))
        assertFalse(SavedMessagesRules.canCall(conv.id))
        assertFalse(SavedMessagesRules.opensPeerProfile(conv.id))
        assertTrue(SavedMessagesRules.skipNetwork(conv.id))
        assertFalse(PeerProfileRules.headerClickable(isGroup = false, hasPeer = true, saved = true))
        assertFalse(PeerProfileRules.opensPeerProfile(isGroup = false, hasPeer = true, saved = true))
        assertTrue(PeerProfileRules.opensPeerProfile(isGroup = false, hasPeer = true, saved = false))
    }

    @Test
    fun previewSkipsYouPrefixAndPresenceIdle() {
        val last = ChatMessage(
            id = "m",
            peerDeviceId = SavedMessagesRules.ID,
            outgoing = true,
            text = "заметка",
            status = MessageStatus.DELIVERED_TO_DEVICE,
            timestampMs = 1L,
            senderId = "me",
        )
        val copy = ChatListPreviewRules.copy(last, draft = "", isGroup = false, myDeviceId = "me", saved = true)
        assertEquals("заметка", copy.text)
        assertEquals(ChatListPreviewKind.LAST, copy.kind)
        assertFalse(copy.text.startsWith("Вы:"))
        val idle = ChatListPreviewRules.copy(null, "", isGroup = false, saved = true)
        assertEquals(SavedMessagesRules.IDLE_SUBTITLE, idle.text)
        assertEquals(ChatListPreviewKind.PRESENCE, idle.kind)
    }

    @Test
    fun emptyThreadCopyAndLocalObjectIds() {
        val idle = ThreadEmptyRules.copy("", saved = true)
        assertEquals(SavedMessagesRules.IDLE_TITLE, idle.title)
        assertEquals(SavedMessagesRules.IDLE_BODY, idle.body)
        val search = ThreadEmptyRules.copy("заметка", saved = true)
        assertEquals("Ничего не найдено", search.title)
        assertTrue(SavedMessagesRules.isLocalObject(SavedMessagesRules.localObjectId("abc")))
        assertFalse(SavedMessagesRules.isLocalObject("server-uuid"))
        assertTrue(SavedMessagesRules.skipNetwork(SavedMessagesRules.ID))
        val from = ForwardRules.originName(
            ChatMessage(
                id = "s",
                peerDeviceId = "peer",
                outgoing = false,
                text = "hi",
                status = MessageStatus.DELIVERED_TO_DEVICE,
                timestampMs = 1L,
                senderName = "Анна",
                kind = MessageKind.TEXT,
            ),
            "Я",
        )
        assertEquals("Анна", from)
        assertEquals("Переслано от Анна", ForwardRules.headerLabel(requireNotNull(from)))
    }

    @Test
    fun pinnedSavedSortsAboveOtherPinned() {
        val saved = SavedMessagesRules.conversation(last = null, prefs = ChatPrefs(pinned = true), myDeviceId = "me")
        val other = Conversation(
            id = "peer",
            title = "Анна",
            subtitle = "hi",
            isGroup = false,
            online = true,
            last = ChatMessage(
                id = "m",
                peerDeviceId = "peer",
                outgoing = false,
                text = "hi",
                status = MessageStatus.DELIVERED_TO_DEVICE,
                timestampMs = 9_999L,
            ),
            pinned = true,
        )
        assertTrue(ChatListRules.compare(saved, other) < 0)
        val rows = ChatListRules.rows(listOf(other, saved), "", ChatListMode.ALL)
        assertEquals(listOf("saved:", "peer"), rows.map { it.id })
    }
}
