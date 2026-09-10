package app.rope.android

import app.rope.android.data.ArchiveRules
import app.rope.android.data.ChatListEmptyRules
import app.rope.android.data.ChatListMode
import app.rope.android.data.ChatListRules
import app.rope.android.data.ChatMessage
import app.rope.android.data.ChatPrefs
import app.rope.android.data.Conversation
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatListUnreadFilterTest {
    private fun conv(
        id: String,
        title: String = id,
        unread: Int = 0,
        muted: Boolean = false,
        archived: Boolean = false,
        group: Boolean = false,
        last: ChatMessage? = null,
    ) = Conversation(
        id = id,
        title = title,
        subtitle = "",
        isGroup = group,
        online = false,
        last = last,
        pinned = false,
        muted = muted,
        unread = unread,
        archived = archived,
    )

    @Test
    fun unreadShowsUnreadIncludingMuted() {
        val mutedUnread = conv("a", title = "Анна", unread = 2, muted = true)
        val loudUnread = conv("b", title = "Боря", unread = 1)
        val read = conv("c", title = "Кира", unread = 0)
        assertTrue(ChatListRules.visible(mutedUnread, ChatListMode.UNREAD))
        assertTrue(ChatListRules.visible(loudUnread, ChatListMode.UNREAD))
        assertFalse(ChatListRules.visible(read, ChatListMode.UNREAD))
        val rows = ChatListRules.rows(listOf(mutedUnread, loudUnread, read), "", ChatListMode.UNREAD)
        assertEquals(listOf("a", "b"), rows.map { it.id }.sorted())
    }

    @Test
    fun unreadHidesSavedAndArchivedUnlessArchiveMode() {
        val saved = SavedMessagesRules.conversation(
            last = null,
            prefs = ChatPrefs(pinned = true),
            myDeviceId = "me",
        )
        val archivedUnread = conv("arch", title = "Архив-чат", unread = 9, archived = true)
        val liveUnread = conv("live", title = "Живой", unread = 3)
        assertFalse(SavedMessagesRules.visible(ChatListMode.UNREAD))
        assertTrue(SavedMessagesRules.visible(ChatListMode.ALL))
        assertFalse(ChatListRules.visible(saved, ChatListMode.UNREAD))
        val unreadSource = ArchiveRules.sourceForList(
            listOf(saved, archivedUnread, liveUnread),
            ChatListMode.UNREAD,
            forwarding = false,
        )
        val unreadRows = ChatListRules.rows(unreadSource, "", ChatListMode.UNREAD)
        assertEquals(listOf("live"), unreadRows.map { it.id })
        assertFalse(unreadRows.any { it.archived })
        val archiveSource = ArchiveRules.sourceForList(
            listOf(archivedUnread, liveUnread),
            ChatListMode.ARCHIVE,
            forwarding = false,
        )
        assertEquals(listOf("arch"), archiveSource.map { it.id })
        assertFalse(ArchiveRules.rowVisible(1, ChatListMode.UNREAD, forwarding = false))
        assertTrue(ArchiveRules.rowVisible(1, ChatListMode.ALL, forwarding = false))
    }

    @Test
    fun chipsAndAppliedMode() {
        assertEquals(ChatListMode.UNREAD, ChatListRules.appliedMode(ChatListMode.ALL, unreadOnly = true))
        assertEquals(ChatListMode.ALL, ChatListRules.appliedMode(ChatListMode.ALL, unreadOnly = false))
        assertEquals(ChatListMode.GROUPS, ChatListRules.appliedMode(ChatListMode.GROUPS, unreadOnly = true))
        assertEquals(ChatListMode.ARCHIVE, ChatListRules.appliedMode(ChatListMode.ARCHIVE, unreadOnly = true))
        assertTrue(ChatListRules.showsFilterChips(ChatListMode.ALL))
        assertFalse(ChatListRules.showsFilterChips(ChatListMode.UNREAD))
        assertFalse(ChatListRules.showsFilterChips(ChatListMode.GROUPS))
        assertFalse(ChatListRules.showsFilterChips(ChatListMode.ARCHIVE))
        assertEquals("Все", ChatListRules.CHIP_ALL)
        assertEquals("Непрочитанные", ChatListRules.CHIP_UNREAD)
        assertEquals(ChatListMode.ALL, NavRules.listMode(Screen.Chats))
    }

    @Test
    fun emptyUnreadCopy() {
        val copy = ChatListEmptyRules.copy(ChatListMode.UNREAD, "", forwarding = false, role = "owner")
        assertEquals("Нет непрочитанных", copy.title)
        assertEquals("Все чаты прочитаны.", copy.body)
        assertFalse(copy.showFab)
        assertFalse(ChatListEmptyRules.showFab(ChatListMode.UNREAD, "", forwarding = false))
        val miss = ChatListEmptyRules.copy(ChatListMode.UNREAD, "анн", forwarding = false, role = "owner")
        assertEquals("Ничего не найдено", miss.title)
        assertEquals("Нет чатов по запросу «анн».", miss.body)
    }

    @Test
    fun unreadCallStillNeedsUnreadCount() {
        val call = ChatMessage("m", "p", true, "звонок", MessageStatus.DELIVERED_TO_DEVICE, 1L, kind = MessageKind.CALL)
        val readCall = conv("c", title = "Звонок", unread = 0, last = call)
        assertTrue(ChatListRules.visible(readCall, ChatListMode.CALLS))
        assertFalse(ChatListRules.visible(readCall, ChatListMode.UNREAD))
    }
}
