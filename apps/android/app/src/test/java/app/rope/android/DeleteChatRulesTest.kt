package app.rope.android

import app.rope.android.data.ArchiveRules
import app.rope.android.data.ChatIds
import app.rope.android.data.ChatListMode
import app.rope.android.data.ChatListRules
import app.rope.android.data.ChatMessage
import app.rope.android.data.ChatPrefs
import app.rope.android.data.Conversation
import app.rope.android.data.DeleteChatRules
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.SavedMessagesRules
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteChatRulesTest {
    private val saved = conv(SavedMessagesRules.ID, title = SavedMessagesRules.TITLE)
    private val anna = conv("aaa111", title = "Анна", unread = 2, subtitle = "привет", pinned = true)
    private val group = conv(ChatIds.group("g-uuid"), title = "Команда", group = true)
    private val gone = anna.copy(deleted = true, pinned = false, unread = 0)

    @Test
    fun savedCannotDeleteOthersCan() {
        assertFalse(DeleteChatRules.canDelete(SavedMessagesRules.ID))
        assertFalse(DeleteChatRules.canDelete(saved))
        assertFalse(DeleteChatRules.canDelete(null))
        assertFalse(DeleteChatRules.canDelete(""))
        assertFalse(DeleteChatRules.canDelete("  "))
        assertTrue(DeleteChatRules.canDelete(anna.id))
        assertTrue(DeleteChatRules.canDelete(group))
    }

    @Test
    fun hideDeletedNotSaved() {
        assertTrue(DeleteChatRules.shouldHide(gone))
        assertFalse(DeleteChatRules.shouldHide(anna))
        assertFalse(DeleteChatRules.shouldHide(saved.copy(deleted = true)))
        val visible = DeleteChatRules.visibleOf(listOf(saved, gone, anna, group))
        assertEquals(listOf(saved.id, anna.id, group.id), visible.map { it.id })
    }

    @Test
    fun listPipelineHidesDeletedBeforeArchive() {
        val archived = anna.copy(archived = true)
        val deletedArchived = archived.copy(deleted = true)
        val all = listOf(saved, deletedArchived, archived, gone)
        val live = DeleteChatRules.visibleOf(all)
        assertTrue(live.none { it.deleted })
        val source = ArchiveRules.sourceForList(live, ChatListMode.ALL, forwarding = false)
        assertEquals(listOf(saved.id), source.map { it.id })
        assertEquals(listOf(archived.id), ArchiveRules.archivedOf(live).map { it.id })
        assertTrue(ArchiveRules.archivedOf(all).any { it.deleted })
        assertEquals(1, ArchiveRules.archivedOf(live).size)
        val rows = ChatListRules.rows(source, "", ChatListMode.ALL)
        assertTrue(rows.none { it.id == anna.id })
        val forward = ArchiveRules.sourceForList(live, ChatListMode.ALL, forwarding = true)
        assertTrue(forward.none { it.deleted })
        assertTrue(forward.any { it.archived })
    }

    @Test
    fun groupsAndCallsSkipDeleted() {
        val liveGroup = conv(ChatIds.group("live"), title = "Живая", group = true)
        val deadGroup = group.copy(deleted = true)
        assertEquals(listOf(liveGroup.id), NavRules.groupsOf(listOf(liveGroup, deadGroup, anna)).map { it.id })
        val call = ChatMessage("m", "p", true, "звонок", MessageStatus.DELIVERED_TO_DEVICE, 1L, kind = MessageKind.CALL)
        val liveCall = conv("dev", title = "Анна", last = call)
        val deadCall = liveCall.copy(id = "dead-call", deleted = true)
        assertEquals(listOf(liveCall.id), NavRules.callsOf(listOf(liveCall, deadCall)).map { it.id })
    }

    @Test
    fun wipePrefsAndRestoreOnMail() {
        val dirty = ChatPrefs(
            pinned = true,
            muted = true,
            unread = 4,
            lastReadMs = 9L,
            draft = "черн",
            pinnedMessageId = "m1",
            archived = true,
        )
        val wiped = DeleteChatRules.clearedPrefs()
        assertTrue(wiped.deleted)
        assertFalse(wiped.pinned)
        assertFalse(wiped.muted)
        assertEquals(0, wiped.unread)
        assertEquals(0L, wiped.lastReadMs)
        assertEquals("", wiped.draft)
        assertEquals(null, wiped.pinnedMessageId)
        assertFalse(wiped.archived)
        val restored = DeleteChatRules.restorePrefs(wiped)
        assertFalse(restored.deleted)
        assertEquals(dirty.copy(deleted = false), DeleteChatRules.restorePrefs(dirty))
        val afterMail = DeleteChatRules.restorePrefs(wiped).copy(unread = wiped.unread + 1)
        assertFalse(afterMail.deleted)
        assertEquals(1, afterMail.unread)
    }

    @Test
    fun promptAndCopy() {
        assertEquals("Удалить чат «Анна»?", DeleteChatRules.prompt("Анна"))
        assertEquals("Удалить чат?", DeleteChatRules.prompt("  "))
        assertEquals("Удалить чат", DeleteChatRules.ACTION)
        assertEquals("Точно удалить", DeleteChatRules.CONFIRM)
        assertTrue(DeleteChatRules.BODY.contains("этом телефоне"))
    }

    @Test
    fun leaveOpenChatOnlyWhenSame() {
        assertTrue(DeleteChatRules.leaveOpenChat("aaa111", "aaa111"))
        assertFalse(DeleteChatRules.leaveOpenChat("other", "aaa111"))
        assertFalse(DeleteChatRules.leaveOpenChat(null, "aaa111"))
        assertFalse(DeleteChatRules.leaveOpenChat("aaa111", ""))
    }

    @Test
    fun prefsRoundTripMissingDeletedIsFalse() {
        val legacy = JSONObject()
            .put("pinned", true)
            .put("muted", false)
            .put("unread", 1)
            .put("last_read_ms", 0)
            .put("draft", "")
            .put("pinned_message", JSONObject.NULL)
            .put("archived", false)
            .toString()
        val parsed = ChatPrefs.parse(legacy)
        assertFalse(parsed.deleted)
        val written = ChatPrefs.parse(ChatPrefs(deleted = true, pinned = true).toJson())
        assertTrue(written.deleted)
        assertTrue(written.pinned)
        assertFalse(ChatPrefs.parse(ChatPrefs().toJson()).deleted)
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
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
        deleted: Boolean = false,
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
        deleted = deleted,
    )
}
