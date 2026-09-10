package app.rope.android

import app.rope.android.data.ArchiveRules
import app.rope.android.data.ChatListEmptyRules
import app.rope.android.data.ChatListMode
import app.rope.android.data.ChatListRules
import app.rope.android.data.ChatMessage
import app.rope.android.data.ChatPrefs
import app.rope.android.data.Conversation
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatListMutedFilterTest {
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
    fun mutedShowsMutedIncludingReadAndExcludesLoud() {
        val mutedRead = conv("a", title = "Анна", unread = 0, muted = true)
        val mutedUnread = conv("b", title = "Боря", unread = 3, muted = true)
        val loud = conv("c", title = "Кира", unread = 4, muted = false)
        assertTrue(ChatListRules.visible(mutedRead, ChatListMode.MUTED))
        assertTrue(ChatListRules.visible(mutedUnread, ChatListMode.MUTED))
        assertFalse(ChatListRules.visible(loud, ChatListMode.MUTED))
        val rows = ChatListRules.rows(listOf(mutedRead, mutedUnread, loud), "", ChatListMode.MUTED)
        assertEquals(listOf("a", "b"), rows.map { it.id }.sorted())
    }

    @Test
    fun mutedHidesSavedAndArchivedUnlessArchiveMode() {
        val saved = SavedMessagesRules.conversation(
            last = null,
            prefs = ChatPrefs(pinned = true),
            myDeviceId = "me",
        )
        val archivedMuted = conv("arch", title = "Архив-чат", muted = true, archived = true)
        val liveMuted = conv("live", title = "Живой", muted = true)
        assertFalse(SavedMessagesRules.visible(ChatListMode.MUTED))
        assertTrue(SavedMessagesRules.visible(ChatListMode.ALL))
        assertFalse(ChatListRules.visible(saved, ChatListMode.MUTED))
        val mutedSource = ArchiveRules.sourceForList(
            listOf(saved, archivedMuted, liveMuted),
            ChatListMode.MUTED,
            forwarding = false,
        )
        val mutedRows = ChatListRules.rows(mutedSource, "", ChatListMode.MUTED)
        assertEquals(listOf("live"), mutedRows.map { it.id })
        assertFalse(mutedRows.any { it.archived })
        val archiveSource = ArchiveRules.sourceForList(
            listOf(archivedMuted, liveMuted),
            ChatListMode.ARCHIVE,
            forwarding = false,
        )
        assertEquals(listOf("arch"), archiveSource.map { it.id })
        assertFalse(ArchiveRules.rowVisible(1, ChatListMode.MUTED, forwarding = false))
        assertTrue(ArchiveRules.rowVisible(1, ChatListMode.ALL, forwarding = false))
    }

    @Test
    fun chipsAndAppliedMutedMode() {
        assertEquals(ChatListMode.MUTED, ChatListRules.appliedMutedMode(ChatListMode.ALL, mutedOnly = true))
        assertEquals(ChatListMode.ALL, ChatListRules.appliedMutedMode(ChatListMode.ALL, mutedOnly = false))
        assertEquals(ChatListMode.GROUPS, ChatListRules.appliedMutedMode(ChatListMode.GROUPS, mutedOnly = true))
        assertEquals(ChatListMode.ARCHIVE, ChatListRules.appliedMutedMode(ChatListMode.ARCHIVE, mutedOnly = true))
        assertTrue(ChatListRules.showsFilterChips(ChatListMode.ALL))
        assertFalse(ChatListRules.showsFilterChips(ChatListMode.MUTED))
        assertFalse(ChatListRules.showsFilterChips(ChatListMode.GROUPS))
        assertFalse(ChatListRules.showsFilterChips(ChatListMode.ARCHIVE))
        assertEquals("Все", ChatListRules.CHIP_ALL)
        assertEquals("Без звука", ChatListRules.CHIP_MUTED)
        assertEquals(ChatListMode.ALL, NavRules.listMode(Screen.Chats))
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun emptyMutedCopy() {
        val copy = ChatListEmptyRules.copy(ChatListMode.MUTED, "", forwarding = false, role = "owner")
        assertEquals("Нет чатов без звука", copy.title)
        assertEquals("Нет заглушенных диалогов.", copy.body)
        assertFalse(copy.showFab)
        assertFalse(ChatListEmptyRules.showFab(ChatListMode.MUTED, "", forwarding = false))
        val miss = ChatListEmptyRules.copy(ChatListMode.MUTED, "анн", forwarding = false, role = "owner")
        assertEquals("Ничего не найдено", miss.title)
        assertEquals("Нет чатов по запросу «анн».", miss.body)
    }

    @Test
    fun mutedIsNotUnreadAndUnreadLoudStaysOut() {
        val loudUnread = conv("c", title = "Звонок", unread = 2, muted = false)
        val mutedRead = conv("m", title = "Тихий", unread = 0, muted = true)
        assertTrue(ChatListRules.visible(loudUnread, ChatListMode.ALL))
        assertFalse(ChatListRules.visible(loudUnread, ChatListMode.MUTED))
        assertTrue(ChatListRules.visible(mutedRead, ChatListMode.MUTED))
        val call = ChatMessage("m", "p", true, "звонок", MessageStatus.DELIVERED_TO_DEVICE, 1L, kind = MessageKind.CALL)
        val mutedCall = conv("k", title = "Звонок", muted = true, last = call)
        assertTrue(ChatListRules.visible(mutedCall, ChatListMode.CALLS))
        assertTrue(ChatListRules.visible(mutedCall, ChatListMode.MUTED))
    }
}
