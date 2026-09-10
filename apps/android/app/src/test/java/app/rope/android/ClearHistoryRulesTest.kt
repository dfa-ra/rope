package app.rope.android

import app.rope.android.data.ChatIds
import app.rope.android.data.ChatPrefs
import app.rope.android.data.ClearHistoryRules
import app.rope.android.data.LocalStore
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClearHistoryRulesTest {
    @Test
    fun canClearAnyNonBlankIncludingSavedAndGroups() {
        assertTrue(ClearHistoryRules.canClear("peer-1"))
        assertTrue(ClearHistoryRules.canClear(SavedMessagesRules.ID))
        assertTrue(ClearHistoryRules.canClear(ChatIds.group("g-uuid")))
        assertFalse(ClearHistoryRules.canClear(null))
        assertFalse(ClearHistoryRules.canClear(""))
        assertFalse(ClearHistoryRules.canClear("  "))
    }

    @Test
    fun afterClearKeepsPinMuteArchiveDropsThreadState() {
        val dirty = ChatPrefs(
            pinned = true,
            muted = true,
            unread = 4,
            lastReadMs = 9L,
            draft = "черн",
            pinnedMessageId = "m1",
            archived = true,
        )
        val next = ClearHistoryRules.afterClear(dirty)
        assertTrue(next.pinned)
        assertTrue(next.muted)
        assertTrue(next.archived)
        assertEquals(0, next.unread)
        assertEquals(0L, next.lastReadMs)
        assertEquals("", next.draft)
        assertEquals(null, next.pinnedMessageId)
        val again = ClearHistoryRules.afterClear(next)
        assertEquals(next, again)
    }

    @Test
    fun copy() {
        assertEquals("Очистить историю", ClearHistoryRules.ACTION)
        assertEquals("Точно очистить", ClearHistoryRules.CONFIRM)
        assertEquals("Отмена", ClearHistoryRules.CANCEL)
        assertTrue(ClearHistoryRules.BODY.contains("этом телефоне"))
        assertTrue(ClearHistoryRules.BODY.contains("останется"))
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
    }
}
