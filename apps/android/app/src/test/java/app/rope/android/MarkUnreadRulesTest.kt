package app.rope.android

import app.rope.android.data.ChatIds
import app.rope.android.data.ChatPrefs
import app.rope.android.data.Conversation
import app.rope.android.data.MarkUnreadRules
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkUnreadRulesTest {
    @Test
    fun savedIsOutEveryoneElseCanMark() {
        assertFalse(MarkUnreadRules.canMark(SavedMessagesRules.ID))
        assertFalse(MarkUnreadRules.canMark(null))
        assertFalse(MarkUnreadRules.canMark("  "))
        assertTrue(MarkUnreadRules.canMark("peer"))
        assertTrue(MarkUnreadRules.canMark(ChatIds.group("g1")))
        val saved = Conversation(
            id = SavedMessagesRules.ID,
            title = SavedMessagesRules.TITLE,
            subtitle = "",
            isGroup = false,
            online = false,
            last = null,
        )
        assertFalse(MarkUnreadRules.canMark(saved))
        assertTrue(MarkUnreadRules.canMark(saved.copy(id = "peer")))
        assertTrue(MarkUnreadRules.canMark(saved.copy(id = "peer", archived = true)))
    }

    @Test
    fun unreadZeroMarksUnreadAsOneWithoutOpening() {
        val cur = ChatPrefs(unread = 0, lastReadMs = 50L, muted = true, pinned = true)
        val next = MarkUnreadRules.markUnread(cur)
        assertEquals(1, next.unread)
        assertEquals(50L, next.lastReadMs)
        assertTrue(next.muted)
        assertTrue(next.pinned)
        assertFalse(next.archived)
        assertEquals("Пометить непрочитанным", MarkUnreadRules.label(0))
        assertFalse(MarkUnreadRules.isUnread(0))
        assertFalse(MarkUnreadRules.isUnread(-3))
        val applied = MarkUnreadRules.apply(cur, nowMs = 99L)
        assertEquals(1, applied.unread)
        assertEquals(50L, applied.lastReadMs)
    }

    @Test
    fun unreadBadgeMarksReadWithoutOpening() {
        val cur = ChatPrefs(unread = 5, lastReadMs = 10L, draft = "черн", archived = true)
        val next = MarkUnreadRules.markRead(cur, nowMs = 77L)
        assertEquals(0, next.unread)
        assertEquals(77L, next.lastReadMs)
        assertEquals("черн", next.draft)
        assertTrue(next.archived)
        assertEquals("Пометить прочитанным", MarkUnreadRules.label(5))
        assertTrue(MarkUnreadRules.isUnread(1))
        val applied = MarkUnreadRules.apply(cur, nowMs = 88L)
        assertEquals(0, applied.unread)
        assertEquals(88L, applied.lastReadMs)
    }

    @Test
    fun applyTogglesAndPrefsRoundTripUnreadOnly() {
        val unread = MarkUnreadRules.apply(ChatPrefs(unread = 0, lastReadMs = 1L), nowMs = 2L)
        assertEquals(1, unread.unread)
        val read = MarkUnreadRules.apply(unread, nowMs = 9L)
        assertEquals(0, read.unread)
        assertEquals(9L, read.lastReadMs)
        val round = ChatPrefs.parse(unread.toJson())
        assertEquals(1, round.unread)
        assertEquals(1L, round.lastReadMs)
        assertEquals(MarkUnreadRules.UNREAD_COUNT, 1)
        assertEquals("Пометить непрочитанным", MarkUnreadRules.MARK_UNREAD)
        assertEquals("Пометить прочитанным", MarkUnreadRules.MARK_READ)
    }
}
