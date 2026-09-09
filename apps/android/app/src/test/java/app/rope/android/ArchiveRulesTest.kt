package app.rope.android

import app.rope.android.data.ArchiveRules
import app.rope.android.data.ArchiveSwipeRules
import app.rope.android.data.ChatIds
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
import app.rope.android.data.UnreadBadgeKind
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveRulesTest {
    private val saved = conv(SavedMessagesRules.ID, title = SavedMessagesRules.TITLE, pinned = true)
    private val anna = conv("aaa111", title = "Анна", unread = 2, subtitle = "привет")
    private val muted = conv("bbb222", title = "Борис", unread = 3, muted = true, archived = true)
    private val group = conv(ChatIds.group("g-uuid"), title = "Команда", group = true, archived = true, unread = 1)
    private val archivedAnna = anna.copy(archived = true, pinned = false)

    @Test
    fun savedCanArchiveFalse() {
        assertFalse(ArchiveRules.canArchive(SavedMessagesRules.ID))
        assertFalse(ArchiveRules.canArchive(saved.id))
        assertTrue(ArchiveRules.canArchive(anna.id))
        assertTrue(ArchiveRules.canArchive(group.id))
        assertFalse(ArchiveSwipeRules.canSwipe(SavedMessagesRules.ID, header = false))
        assertTrue(ArchiveSwipeRules.canSwipe(anna.id, header = false))
    }

    @Test
    fun archiveWritesPinnedFalse() {
        val pinned = ChatPrefs(pinned = true, muted = true, unread = 4)
        val archived = ArchiveRules.archivePrefs(pinned)
        assertTrue(archived.archived)
        assertFalse(archived.pinned)
        assertTrue(archived.muted)
        assertEquals(4, archived.unread)
        assertTrue(ArchiveRules.unpinOnArchive(true))
        assertTrue(ArchiveRules.unpinOnArchive(false))
        val restored = ArchiveRules.unarchivePrefs(archived)
        assertFalse(restored.archived)
        assertFalse(restored.pinned)
    }

    @Test
    fun newMailKeepsArchivedAndIncrementsUnread() {
        val cur = ChatPrefs(archived = true, unread = 2, pinned = false)
        val after = cur.copy(unread = cur.unread + 1)
        assertTrue(after.archived)
        assertEquals(3, after.unread)
        assertFalse(after.pinned)
    }

    @Test
    fun enterChatDoesNotUnarchive() {
        val cur = ChatPrefs(archived = true, unread = 5, pinned = false)
        val opened = cur.copy(unread = 0, lastReadMs = 99L)
        assertTrue(opened.archived)
        assertEquals(0, opened.unread)
        assertEquals(99L, opened.lastReadMs)
    }

    @Test
    fun badgeGrayWhenAllMuted() {
        val mutedOnly = listOf(muted)
        assertEquals(UnreadBadgeKind.MUTED, ArchiveRules.badgeKind(mutedOnly))
        assertEquals(3, ArchiveRules.unreadSum(mutedOnly))
        val mixed = listOf(muted, archivedAnna)
        assertEquals(UnreadBadgeKind.ACCENT, ArchiveRules.badgeKind(mixed))
        assertEquals(5, ArchiveRules.unreadSum(mixed))
        assertEquals(UnreadBadgeKind.NONE, ArchiveRules.badgeKind(listOf(archivedAnna.copy(unread = 0))))
    }

    @Test
    fun allSearchMissesArchived() {
        val all = listOf(saved, anna.copy(archived = true), conv("ccc", title = "Кира"))
        val source = ArchiveRules.sourceForList(all, ChatListMode.ALL, forwarding = false)
        val rows = ChatListRules.rows(source, "анн", ChatListMode.ALL)
        assertTrue(rows.none { it.title == "Анна" })
        assertTrue(source.none { ArchiveRules.shouldHideFromMain(it) })
        assertTrue(source.any { SavedMessagesRules.isSaved(it.id) })
    }

    @Test
    fun archiveSearchHits() {
        val all = listOf(saved, archivedAnna, conv("ccc", title = "Кира"))
        val source = ArchiveRules.sourceForList(all, ChatListMode.ARCHIVE, forwarding = false)
        val rows = ChatListRules.rows(source, "анн", ChatListMode.ARCHIVE)
        assertEquals(listOf(archivedAnna.id), rows.map { it.id })
        assertFalse(source.any { SavedMessagesRules.isSaved(it.id) })
    }

    @Test
    fun groupsHidesArchived() {
        val liveGroup = conv(ChatIds.group("live"), title = "Живая", group = true)
        val all = listOf(liveGroup, group, anna)
        val source = ArchiveRules.sourceForList(all, ChatListMode.GROUPS, forwarding = false)
        val rows = ChatListRules.rows(source, "", ChatListMode.GROUPS)
        assertEquals(listOf(liveGroup.id), rows.map { it.id })
        assertTrue(NavRules.groupsOf(all).none { it.archived })
        val call = ChatMessage("m", "p", true, "звонок", MessageStatus.DELIVERED_TO_DEVICE, 1L, kind = MessageKind.CALL)
        val liveCall = conv("dev", title = "Анна", last = call)
        val archivedCall = liveCall.copy(id = "arch-call", archived = true)
        assertEquals(listOf(liveCall.id), NavRules.callsOf(listOf(liveCall, archivedCall)).map { it.id })
    }

    @Test
    fun folderApplyDoesNotSeeArchived() {
        val all = listOf(saved, archivedAnna, conv("ccc", title = "Кира", unread = 1))
        val notArchived = ArchiveRules.sourceForList(all, ChatListMode.ALL, forwarding = false)
        val unread = notArchived.filter { it.unread > 0 }
        assertTrue(unread.none { it.archived })
        assertEquals(listOf("ccc"), unread.map { it.id })
        assertFalse(notArchived.any { it.id == archivedAnna.id })
    }

    @Test
    fun unreadFolderExcludesArchived() {
        val all = listOf(archivedAnna.copy(unread = 9), conv("ccc", title = "Кира", unread = 1))
        val base = ChatListRules.rows(
            ArchiveRules.sourceForList(all, ChatListMode.ALL, forwarding = false),
            "",
            ChatListMode.ALL,
        )
        val unreadFolder = base.filter { it.unread > 0 }
        assertEquals(1, unreadFolder.size)
        assertEquals("ccc", unreadFolder.single().id)
        assertEquals(9, ArchiveRules.unreadSum(ArchiveRules.archivedOf(all)))
        assertTrue(ArchiveRules.rowVisible(ArchiveRules.archivedOf(all).size, ChatListMode.ALL, forwarding = false))
        assertFalse(ArchiveRules.rowVisible(1, ChatListMode.GROUPS, forwarding = false))
        assertFalse(ArchiveRules.rowVisible(1, ChatListMode.ALL, forwarding = true))
        assertFalse(ArchiveRules.rowVisible(0, ChatListMode.ALL, forwarding = false))
    }

    @Test
    fun canSwipeFalseForSavedAndHeader() {
        assertFalse(ArchiveSwipeRules.canSwipe(SavedMessagesRules.ID, header = false))
        assertFalse(ArchiveSwipeRules.canSwipe(anna.id, header = true))
        assertFalse(ArchiveSwipeRules.canSwipe(null, header = false))
        assertTrue(ArchiveSwipeRules.canSwipe(anna.id, header = false))
        assertTrue(ArchiveSwipeRules.stillSettled(0f, 0f))
        assertTrue(ArchiveSwipeRules.shouldLock(-ArchiveSwipeRules.SLOP_DP, 0f))
        assertTrue(ArchiveSwipeRules.shouldAbort(0f, 20f))
        assertTrue(ArchiveSwipeRules.shouldCommit(-ArchiveSwipeRules.COMMIT_DP))
        assertFalse(ArchiveSwipeRules.shouldCommit(-ArchiveSwipeRules.COMMIT_DP + 1f))
    }

    @Test
    fun forwardKeepsArchived() {
        val all = listOf(saved, archivedAnna)
        val source = ArchiveRules.sourceForList(all, ChatListMode.ALL, forwarding = true)
        assertTrue(source.any { it.id == archivedAnna.id })
        assertFalse(ArchiveRules.rowVisible(1, ChatListMode.ALL, forwarding = true))
    }

    @Test
    fun savedStaysVisibleIfFlagSet() {
        val flagged = saved.copy(archived = true)
        assertFalse(ArchiveRules.shouldHideFromMain(flagged))
        val source = ArchiveRules.sourceForList(listOf(flagged, archivedAnna), ChatListMode.ALL, forwarding = false)
        assertTrue(source.any { SavedMessagesRules.isSaved(it.id) })
        assertFalse(source.any { it.id == archivedAnna.id })
    }

    @Test
    fun canPinSavedNotArchived() {
        assertTrue(ArchiveRules.canPin(saved))
        assertFalse(ArchiveRules.canPin(archivedAnna))
        assertTrue(ArchiveRules.canPin(ChatPrefs(pinned = true)))
        assertFalse(ArchiveRules.canPin(ChatPrefs(archived = true)))
    }

    @Test
    fun prefsRoundTripMissingArchivedIsFalse() {
        val legacy = JSONObject()
            .put("pinned", true)
            .put("muted", false)
            .put("unread", 1)
            .put("last_read_ms", 0)
            .put("draft", "")
            .put("pinned_message", JSONObject.NULL)
            .toString()
        val parsed = ChatPrefs.parse(legacy)
        assertFalse(parsed.archived)
        val written = ChatPrefs.parse(ChatPrefs(archived = true, pinned = true).toJson())
        assertTrue(written.archived)
        assertTrue(written.pinned)
        assertFalse(ArchiveRules.archivePrefs(ChatPrefs(pinned = true)).pinned)
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun previewFallsBackToCount() {
        val emptyLast = archivedAnna.copy(last = null, subtitle = "")
        assertEquals("1 ${ArchiveRules.COUNT_SUFFIX}", ArchiveRules.preview(listOf(emptyLast)))
        val withLast = archivedAnna.copy(
            last = ChatMessage("m", "p", false, "hi", MessageStatus.DELIVERED_TO_DEVICE, 9L),
            subtitle = "hi",
        )
        assertEquals("hi", ArchiveRules.preview(listOf(withLast, emptyLast)))
    }

    @Test
    fun emptyArchiveCopy() {
        val copy = ChatListEmptyRules.copy(ChatListMode.ARCHIVE, "", forwarding = false, role = "owner")
        assertEquals(ArchiveRules.EMPTY_TITLE, copy.title)
        assertEquals(ArchiveRules.DEVICE_ONLY, copy.body)
        assertFalse(copy.showFab)
        assertNull(copy.actionLabel)
        assertFalse(ChatListEmptyRules.showFab(ChatListMode.ARCHIVE, "", forwarding = false))
        val miss = ChatListEmptyRules.copy(ChatListMode.ARCHIVE, "анн", forwarding = false, role = "owner")
        assertEquals(ChatListEmptyRules.SEARCH_TITLE, miss.title)
        assertTrue(miss.body.contains("чатов"))
    }

    private fun assertNull(value: Any?) {
        org.junit.Assert.assertNull(value)
    }

    private fun conv(
        id: String,
        title: String = id,
        unread: Int = 0,
        muted: Boolean = false,
        archived: Boolean = false,
        pinned: Boolean = false,
        group: Boolean = false,
        subtitle: String = "",
        last: ChatMessage? = null,
    ): Conversation = Conversation(
        id = id,
        title = title,
        subtitle = subtitle,
        isGroup = group,
        online = false,
        last = last,
        pinned = pinned,
        muted = muted,
        unread = unread,
        archived = archived,
    )
}
