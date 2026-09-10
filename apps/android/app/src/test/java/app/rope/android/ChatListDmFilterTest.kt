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

class ChatListDmFilterTest {
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
    fun dmShowsOneToOneAndSavedHidesGroups() {
        val dm = conv("a", title = "Анна")
        val group = conv("g", title = "Команда", group = true)
        val saved = SavedMessagesRules.conversation(
            last = null,
            prefs = ChatPrefs(pinned = true),
            myDeviceId = "me",
        )
        assertTrue(ChatListRules.visible(dm, ChatListMode.DM))
        assertFalse(ChatListRules.visible(group, ChatListMode.DM))
        assertTrue(ChatListRules.visible(saved, ChatListMode.DM))
        assertTrue(SavedMessagesRules.visible(ChatListMode.DM))
        val rows = ChatListRules.rows(listOf(dm, group, saved), "", ChatListMode.DM)
        assertEquals(listOf("saved:", "a"), rows.map { it.id })
    }

    @Test
    fun dmHidesArchivedUnlessArchiveMode() {
        val archivedDm = conv("arch", title = "Архив-чат", archived = true)
        val liveDm = conv("live", title = "Живой")
        val liveGroup = conv("g", title = "Группа", group = true)
        val dmSource = ArchiveRules.sourceForList(
            listOf(archivedDm, liveDm, liveGroup),
            ChatListMode.DM,
            forwarding = false,
        )
        val dmRows = ChatListRules.rows(dmSource, "", ChatListMode.DM)
        assertEquals(listOf("live"), dmRows.map { it.id })
        assertFalse(dmRows.any { it.archived || it.isGroup })
        val archiveSource = ArchiveRules.sourceForList(
            listOf(archivedDm, liveDm),
            ChatListMode.ARCHIVE,
            forwarding = false,
        )
        assertEquals(listOf("arch"), archiveSource.map { it.id })
        assertFalse(ArchiveRules.rowVisible(1, ChatListMode.DM, forwarding = false))
        assertTrue(ArchiveRules.rowVisible(1, ChatListMode.ALL, forwarding = false))
    }

    @Test
    fun chipsAndAppliedDmMode() {
        assertEquals(ChatListMode.DM, ChatListRules.appliedDmMode(ChatListMode.ALL, dmOnly = true))
        assertEquals(ChatListMode.ALL, ChatListRules.appliedDmMode(ChatListMode.ALL, dmOnly = false))
        assertEquals(ChatListMode.GROUPS, ChatListRules.appliedDmMode(ChatListMode.GROUPS, dmOnly = true))
        assertEquals(ChatListMode.ARCHIVE, ChatListRules.appliedDmMode(ChatListMode.ARCHIVE, dmOnly = true))
        assertTrue(ChatListRules.showsFilterChips(ChatListMode.ALL))
        assertFalse(ChatListRules.showsFilterChips(ChatListMode.DM))
        assertFalse(ChatListRules.showsFilterChips(ChatListMode.GROUPS))
        assertFalse(ChatListRules.showsFilterChips(ChatListMode.ARCHIVE))
        assertEquals("Все", ChatListRules.CHIP_ALL)
        assertEquals("Личные", ChatListRules.CHIP_DM)
        assertEquals(ChatListMode.ALL, NavRules.listMode(Screen.Chats))
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun emptyDmCopy() {
        val copy = ChatListEmptyRules.copy(ChatListMode.DM, "", forwarding = false, role = "owner")
        assertEquals("Нет личных чатов", copy.title)
        assertEquals("Нет диалогов один на один.", copy.body)
        assertFalse(copy.showFab)
        assertFalse(ChatListEmptyRules.showFab(ChatListMode.DM, "", forwarding = false))
        val miss = ChatListEmptyRules.copy(ChatListMode.DM, "анн", forwarding = false, role = "owner")
        assertEquals("Ничего не найдено", miss.title)
        assertEquals("Нет чатов по запросу «анн».", miss.body)
    }

    @Test
    fun dmCallStaysAndGroupCallDrops() {
        val call = ChatMessage("m", "p", true, "звонок", MessageStatus.DELIVERED_TO_DEVICE, 1L, kind = MessageKind.CALL)
        val dmCall = conv("k", title = "Кира", last = call)
        val groupCall = conv("g", title = "Созвон", group = true, last = call)
        assertTrue(ChatListRules.visible(dmCall, ChatListMode.CALLS))
        assertTrue(ChatListRules.visible(dmCall, ChatListMode.DM))
        assertTrue(ChatListRules.visible(groupCall, ChatListMode.CALLS))
        assertFalse(ChatListRules.visible(groupCall, ChatListMode.DM))
    }
}
