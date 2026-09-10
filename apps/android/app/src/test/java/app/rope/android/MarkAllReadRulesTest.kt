package app.rope.android

import app.rope.android.data.ChatIds
import app.rope.android.data.ChatListMode
import app.rope.android.data.ChatListRules
import app.rope.android.data.ChatPrefs
import app.rope.android.data.Conversation
import app.rope.android.data.LocalStore
import app.rope.android.data.MarkAllReadRules
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkAllReadRulesTest {
    private val anna = conv("aaa111", title = "Анна", unread = 2)
    private val boris = conv("bbb222", title = "Борис", unread = 0)
    private val group = conv(ChatIds.group("g-uuid"), title = "Команда", unread = 4, group = true)
    private val saved = conv(SavedMessagesRules.ID, title = SavedMessagesRules.TITLE, unread = 9, pinned = true)

    @Test
    fun skipsBlankSavedAndReadChats() {
        assertFalse(MarkAllReadRules.canMark(null))
        assertFalse(MarkAllReadRules.canMark(""))
        assertFalse(MarkAllReadRules.canMark("  "))
        assertFalse(MarkAllReadRules.canMark(SavedMessagesRules.ID))
        assertTrue(MarkAllReadRules.canMark(anna.id))
        assertTrue(MarkAllReadRules.canMark(group.id))
        assertEquals(listOf(anna.id, group.id), MarkAllReadRules.ids(listOf(anna, boris, group, saved)))
        assertEquals(emptyList<String>(), MarkAllReadRules.ids(listOf(boris, saved)))
    }

    @Test
    fun groupsTabOnlyMarksUnreadGroups() {
        val source = listOf(anna, group, saved)
        val rows = ChatListRules.rows(source, "", ChatListMode.GROUPS)
        assertEquals(listOf(group.id), MarkAllReadRules.ids(rows))
    }

    @Test
    fun hiddenWhileForwardingOrSearching() {
        assertFalse(MarkAllReadRules.visible(0, forwarding = false, searching = false))
        assertTrue(MarkAllReadRules.visible(1, forwarding = false, searching = false))
        assertFalse(MarkAllReadRules.visible(2, forwarding = true, searching = false))
        assertFalse(MarkAllReadRules.visible(2, forwarding = false, searching = true))
        assertTrue(ChatListRules.searching("анн"))
        assertFalse(ChatListRules.searching("  "))
    }

    @Test
    fun prefsClearUnreadAndKeepMutePinArchive() {
        val cur = ChatPrefs(pinned = true, muted = true, unread = 7, lastReadMs = 1L, draft = "черн", archived = true)
        val after = MarkAllReadRules.prefsAfter(cur, nowMs = 99L)
        assertEquals(0, after.unread)
        assertEquals(99L, after.lastReadMs)
        assertTrue(after.pinned)
        assertTrue(after.muted)
        assertTrue(after.archived)
        assertEquals("черн", after.draft)
        assertEquals(MarkAllReadRules.ACTION, "Прочитать все")
        assertEquals(6, LocalStore.VERSION)
    }

    private fun conv(
        id: String,
        title: String,
        unread: Int = 0,
        pinned: Boolean = false,
        group: Boolean = false,
    ): Conversation = Conversation(
        id = id,
        title = title,
        subtitle = "",
        isGroup = group,
        online = false,
        last = null,
        pinned = pinned,
        unread = unread,
    )
}
