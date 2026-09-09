package app.rope.android

import app.rope.android.data.ChatIds
import app.rope.android.data.ChatPrefs
import app.rope.android.data.MentionBadgeRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MentionBadgeRulesTest {
    @Test
    fun nameAndBroadcastHitOnlyInGroups() {
        assertTrue(MentionBadgeRules.mentionsMe("Эй, @Аня смотри", "Аня"))
        assertTrue(MentionBadgeRules.mentionsMe("пинг @all", "Аня"))
        assertTrue(MentionBadgeRules.mentionsMe("всем @всем сюда", "Аня"))
        assertFalse(MentionBadgeRules.mentionsMe("Эй, @Боря", "Аня"))
        assertFalse(MentionBadgeRules.mentionsMe("без собаки", "Аня"))
        assertTrue(MentionBadgeRules.hit(true, "Эй, @Аня", "Аня"))
        assertFalse(MentionBadgeRules.hit(false, "Эй, @Аня", "Аня"))
        assertTrue(ChatIds.isGroup("g:abc"))
        assertFalse(ChatIds.isGroup("peer-1"))
    }

    @Test
    fun hiddenPrefsBumpMentionsAndOpenClears() {
        val cur = ChatPrefs(unread = 2, unreadMentions = 1, muted = true)
        val hit = MentionBadgeRules.afterHidden(cur, isGroup = true, body = "смотри @Аня", myName = "Аня")
        assertEquals(3, hit.unread)
        assertEquals(2, hit.unreadMentions)
        assertTrue(hit.muted)
        val miss = MentionBadgeRules.afterHidden(cur, isGroup = true, body = "ок", myName = "Аня")
        assertEquals(3, miss.unread)
        assertEquals(1, miss.unreadMentions)
        val dm = MentionBadgeRules.afterHidden(cur, isGroup = false, body = "@Аня", myName = "Аня")
        assertEquals(3, dm.unread)
        assertEquals(1, dm.unreadMentions)
        val opened = MentionBadgeRules.afterOpened(hit, nowMs = 50L)
        assertEquals(0, opened.unread)
        assertEquals(0, opened.unreadMentions)
        assertEquals(50L, opened.lastReadMs)
        assertTrue(opened.muted)
    }

    @Test
    fun jsonRoundTripAndShowMark() {
        val written = ChatPrefs.parse(
            ChatPrefs(unread = 4, unreadMentions = 2, archived = true).toJson(),
        )
        assertEquals(4, written.unread)
        assertEquals(2, written.unreadMentions)
        assertTrue(written.archived)
        assertEquals(0, ChatPrefs.parse("""{"unread":1}""").unreadMentions)
        assertTrue(MentionBadgeRules.showMark(true))
        assertFalse(MentionBadgeRules.showMark(false))
        assertTrue(MentionBadgeRules.mentioned(1))
        assertFalse(MentionBadgeRules.mentioned(0))
        assertEquals("@", MentionBadgeRules.MARK)
        assertEquals("Упоминание", MentionBadgeRules.LABEL)
        assertFalse(MentionBadgeRules.LABEL.contains("FCM", ignoreCase = true))
    }
}
