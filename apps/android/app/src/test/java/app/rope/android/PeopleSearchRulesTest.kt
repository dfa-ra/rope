package app.rope.android

import app.rope.android.data.ChatListHit
import app.rope.android.data.DirectoryDevice
import app.rope.android.data.LocalStore
import app.rope.android.data.PeopleSearchRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PeopleSearchRulesTest {
    @Test
    fun inheritStoreAndBlankQueryKeepsOrder() {
        assertEquals(6, LocalStore.VERSION)
        val anna = person("a", "Мария Анна")
        val bob = person("b", "Боб")
        val people = listOf(anna, bob)
        assertFalse(PeopleSearchRules.searching("  "))
        assertTrue(PeopleSearchRules.showSearch(people))
        assertFalse(PeopleSearchRules.showSearch(emptyList()))
        assertEquals(people, PeopleSearchRules.rows(people, "  "))
    }

    @Test
    fun wordPrefixBeatsContainsAndMissesIdleCopy() {
        val maria = person("a", "Мария Анна")
        val marina = person("b", "Марианна")
        val bob = person("c", "Боб")
        val people = listOf(bob, maria, marina)
        val rows = PeopleSearchRules.rows(people, "  Анн  ")
        assertEquals(listOf("a", "b"), rows.map { it.deviceId })
        assertEquals(ChatListHit.TITLE_PREFIX, PeopleSearchRules.hit(maria, "анн"))
        assertEquals(ChatListHit.TITLE, PeopleSearchRules.hit(marina, "анн"))
        assertFalse(PeopleSearchRules.matches(bob, "анн"))
        assertEquals("Ничего не найдено", PeopleSearchRules.SEARCH_TITLE)
        assertEquals("Нет людей по запросу «анн».", PeopleSearchRules.searchBody("  Анн  "))
        assertEquals("Поиск", PeopleSearchRules.PLACEHOLDER)
    }

    @Test
    fun blankDisplayNameFallsBackToDevicePrefix() {
        val anon = person("abcd1234", "")
        assertEquals("abcd1234", PeopleSearchRules.displayName(anon).take(8))
        assertTrue(PeopleSearchRules.matches(anon, "abcd"))
        assertFalse(PeopleSearchRules.matches(anon, "xyz"))
    }

    @Test
    fun longQueryFallsBackWithoutIdleHint() {
        val q = "x".repeat(90)
        val body = PeopleSearchRules.searchBody(q)
        assertEquals(PeopleSearchRules.SEARCH_BODY_FALLBACK, body)
        assertFalse(body.contains("Пока никого"))
    }

    private fun person(id: String, name: String): DirectoryDevice =
        DirectoryDevice(id, "m$id", name, ByteArray(0), "", online = false)
}
