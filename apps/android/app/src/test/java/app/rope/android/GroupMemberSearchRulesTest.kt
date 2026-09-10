package app.rope.android

import app.rope.android.data.ChatListHit
import app.rope.android.data.GroupChatUx
import app.rope.android.data.GroupMemberSearchRules
import app.rope.android.data.LocalStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupMemberSearchRulesTest {
    @Test
    fun inheritStoreAndBlankQueryKeepsOrder() {
        assertEquals(6, LocalStore.VERSION)
        val ids = listOf("a", "b", "me")
        val names = mapOf("a" to "Мария Анна", "b" to "Боб", "me" to "Я")
        assertFalse(GroupMemberSearchRules.searching("  "))
        assertTrue(GroupMemberSearchRules.showSearch(ids.size))
        assertFalse(GroupMemberSearchRules.showSearch(0))
        assertEquals(ids, GroupMemberSearchRules.rows(ids, "me", names, "  "))
        assertEquals(GroupChatUx.YOU, GroupMemberSearchRules.displayName("me", "me", names))
        assertEquals("Поиск", GroupMemberSearchRules.PLACEHOLDER)
    }

    @Test
    fun wordPrefixBeatsContainsAndMissesIdleCopy() {
        val ids = listOf("c", "a", "b")
        val names = mapOf("a" to "Мария Анна", "b" to "Марианна", "c" to "Боб")
        val rows = GroupMemberSearchRules.rows(ids, "me", names, "  Анн  ")
        assertEquals(listOf("a", "b"), rows)
        assertEquals(ChatListHit.TITLE_PREFIX, GroupMemberSearchRules.hit("Мария Анна", "анн"))
        assertEquals(ChatListHit.TITLE, GroupMemberSearchRules.hit("Марианна", "анн"))
        assertFalse(GroupMemberSearchRules.matches("Боб", "анн"))
        assertEquals("Ничего не найдено", GroupMemberSearchRules.SEARCH_TITLE)
        assertEquals("Нет участников по запросу «анн».", GroupMemberSearchRules.searchBody("  Анн  "))
    }

    @Test
    fun selfYouAndBlankNameFallback() {
        val ids = listOf("abcd1234", "me")
        val names = mapOf("abcd1234" to "", "me" to "Боря")
        assertEquals("abcd1234", GroupMemberSearchRules.displayName("abcd1234", "me", names).take(8))
        assertTrue(GroupMemberSearchRules.matches("abcd1234", "abcd"))
        assertTrue(GroupMemberSearchRules.matches(GroupChatUx.YOU, "вы"))
        val youHits = GroupMemberSearchRules.rows(ids, "me", names, "вы")
        assertEquals(listOf("me"), youHits)
        val q = "x".repeat(90)
        assertEquals(GroupMemberSearchRules.SEARCH_BODY_FALLBACK, GroupMemberSearchRules.searchBody(q))
    }
}
