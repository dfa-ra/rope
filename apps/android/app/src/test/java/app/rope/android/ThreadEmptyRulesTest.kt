package app.rope.android

import app.rope.android.data.ChatListEmptyRules
import app.rope.android.data.RoleRules
import app.rope.android.data.ThreadEmptyRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadEmptyRulesTest {
    @Test
    fun searchMissUnifiesToFoundNotNashli() {
        val copy = ThreadEmptyRules.copy("  Анн  ")
        assertEquals("Ничего не найдено", copy.title)
        assertEquals(ChatListEmptyRules.SEARCH_TITLE, copy.title)
        assertFalse(copy.title.contains("нашли"))
        assertEquals("Нет сообщений по запросу «анн».", copy.body)
    }

    @Test
    fun whitespaceQueryIsIdleNotSearch() {
        val copy = ThreadEmptyRules.copy(" \t ")
        assertEquals("Начните переписку", copy.title)
        assertEquals(RoleRules.threadEmptyBody(), copy.body)
        assertFalse(copy.title.contains("найдено"))
        assertFalse(copy.body.contains("запросу"))
    }

    @Test
    fun idleKeepsStartTheChat() {
        val copy = ThreadEmptyRules.copy("")
        assertEquals(ThreadEmptyRules.IDLE_TITLE, copy.title)
        assertEquals("Напишите сообщение", copy.body)
    }

    @Test
    fun longQueryFallsBackWithoutLectures() {
        val q = "я".repeat(70)
        val body = ThreadEmptyRules.searchBody(q)
        assertEquals("Попробуйте изменить запрос.", body)
        assertTrue(body.length <= ThreadEmptyRules.COPY_MAX)
        assertFalse(body.contains('\n'))
        assertFalse(body.contains("нашли"))
    }

    @Test
    fun kindChipWithoutQueryIsStillAMiss() {
        val copy = ThreadEmptyRules.copy("", kind = app.rope.android.data.MessageSearchKind.PHOTO)
        assertEquals("Ничего не найдено", copy.title)
        assertEquals("Нет фото в этом чате.", copy.body)
    }

    @Test
    fun searchTitleNeverUsesColloquialNashli() {
        val copy = ThreadEmptyRules.copy("фото")
        assertEquals("Ничего не найдено", copy.title)
        assertEquals("Нет сообщений по запросу «фото».", copy.body)
        assertFalse(copy.body.contains("Другой запрос"))
    }
}
