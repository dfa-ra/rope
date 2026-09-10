package app.rope.android

import app.rope.android.data.ArchiveRules
import app.rope.android.data.ChatListEmptyRules
import app.rope.android.data.ChatListMode
import app.rope.android.data.ChatListPreviewRules
import app.rope.android.data.ChatListRules
import app.rope.android.data.ChatMessage
import app.rope.android.data.ChatPrefs
import app.rope.android.data.Conversation
import app.rope.android.data.DraftFilterRules
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftFilterRulesTest {
    private fun conv(
        id: String,
        draft: String = "",
        group: Boolean = false,
        archived: Boolean = false,
        last: ChatMessage? = null,
    ) = Conversation(
        id = id,
        title = id,
        subtitle = if (ChatListPreviewRules.clip(draft).isNotEmpty()) {
            "${ChatListPreviewRules.DRAFT_LABEL}: ${ChatListPreviewRules.clip(draft)}"
        } else {
            ""
        },
        isGroup = group,
        online = false,
        last = last,
        draft = draft,
        archived = archived,
    )

    @Test
    fun inheritStoreAndChipCopy() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("Черновики", DraftFilterRules.CHIP)
        assertEquals("Все", DraftFilterRules.CHIP_ALL)
        assertEquals("Нет черновиков", DraftFilterRules.EMPTY)
        assertFalse(DraftFilterRules.CHIP.contains("FCM", ignoreCase = true))
    }

    @Test
    fun blankAndWhitespaceAreNotDrafts() {
        assertFalse(DraftFilterRules.hasDraft(""))
        assertFalse(DraftFilterRules.hasDraft("  \t "))
        assertTrue(DraftFilterRules.hasDraft("привет"))
        assertTrue(DraftFilterRules.hasDraft("  ок  "))
        assertFalse(DraftFilterRules.hasDraft(conv("a")))
        assertTrue(DraftFilterRules.hasDraft(conv("b", draft = "черн")))
    }

    @Test
    fun applyKeepsOnlyDraftRows() {
        val live = conv("a")
        val drafted = conv("b", draft = "набираю")
        val groupDraft = conv("g", draft = "всем", group = true)
        val rows = DraftFilterRules.apply(listOf(live, drafted, groupDraft), draftsOnly = true)
        assertEquals(listOf("b", "g"), rows.map { it.id })
        assertEquals(listOf("a", "b", "g"), DraftFilterRules.apply(listOf(live, drafted, groupDraft), false).map { it.id })
    }

    @Test
    fun chipsOnlyOnAllChatsTab() {
        assertTrue(DraftFilterRules.showsChips(ChatListMode.ALL))
        assertFalse(DraftFilterRules.showsChips(ChatListMode.GROUPS))
        assertFalse(DraftFilterRules.showsChips(ChatListMode.CALLS))
        assertFalse(DraftFilterRules.showsChips(ChatListMode.ARCHIVE))
    }

    @Test
    fun savedWithDraftStays() {
        val saved = SavedMessagesRules.conversation(
            last = null,
            prefs = ChatPrefs(pinned = true, draft = "заметка"),
            myDeviceId = "me",
        )
        assertTrue(DraftFilterRules.hasDraft(saved))
        assertEquals(listOf(SavedMessagesRules.ID), DraftFilterRules.apply(listOf(saved), true).map { it.id })
        val idle = SavedMessagesRules.conversation(null, ChatPrefs(pinned = true), "me")
        assertFalse(DraftFilterRules.hasDraft(idle))
    }

    @Test
    fun archiveRowHiddenAndEmptyCopy() {
        assertTrue(DraftFilterRules.hideArchiveRow(true))
        assertFalse(DraftFilterRules.hideArchiveRow(false))
        assertFalse(ArchiveRules.rowVisible(1, ChatListMode.ALL, forwarding = false) && DraftFilterRules.hideArchiveRow(true))
        assertFalse(DraftFilterRules.showFab(true, ChatListMode.ALL, "", forwarding = false))
        assertTrue(DraftFilterRules.showFab(false, ChatListMode.ALL, "", forwarding = false))
        val empty = DraftFilterRules.emptyCopy(true, ChatListMode.ALL, "", forwarding = false, role = "owner")
        assertEquals(DraftFilterRules.EMPTY, empty.title)
        assertEquals(DraftFilterRules.EMPTY_BODY, empty.body)
        assertFalse(empty.showFab)
        val miss = DraftFilterRules.emptyCopy(true, ChatListMode.ALL, "анн", forwarding = false, role = "owner")
        assertEquals(ChatListEmptyRules.SEARCH_TITLE, miss.title)
        assertEquals("Нет черновиков по запросу «анн».", miss.body)
        val all = DraftFilterRules.emptyCopy(false, ChatListMode.ALL, "", forwarding = false, role = "owner")
        assertEquals("Пока никого нет", all.title)
    }

    @Test
    fun draftsSurviveListPipelineOnAll() {
        val call = ChatMessage("m", "p", true, "звонок", MessageStatus.DELIVERED_TO_DEVICE, 1L, kind = MessageKind.CALL)
        val drafted = conv("d", draft = "черн", last = call)
        val archivedDraft = conv("arch", draft = "спрятан", archived = true)
        val source = ArchiveRules.sourceForList(listOf(drafted, archivedDraft), ChatListMode.ALL, forwarding = false)
        val rows = ChatListRules.rows(DraftFilterRules.apply(source, true), "", ChatListMode.ALL)
        assertEquals(listOf("d"), rows.map { it.id })
        assertTrue(ChatListPreviewRules.isDraft(drafted.subtitle))
    }
}
