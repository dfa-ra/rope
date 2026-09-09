package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.MessageStatus
import app.rope.android.data.SearchJumpRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchJumpRulesTest {
    private fun msg(
        id: String,
        text: String,
        deleted: Boolean = false,
        sender: String = "",
    ) = ChatMessage(
        id,
        "p",
        false,
        text,
        MessageStatus.DELIVERED_TO_DEVICE,
        1L,
        senderName = sender,
        deleted = deleted,
    )

    @Test
    fun blankQueryHasNoHits() {
        val msgs = listOf(msg("a", "привет"), msg("b", "пока"))
        assertEquals(emptyList<String>(), SearchJumpRules.hits(msgs, ""))
        assertEquals(emptyList<String>(), SearchJumpRules.hits(msgs, "  \t"))
        assertFalse(SearchJumpRules.keepPlace(" "))
        assertEquals(SearchJumpRules.NONE, SearchJumpRules.label(emptyList(), null))
    }

    @Test
    fun hitsSkipDeletedAndKeepOrder() {
        val msgs = listOf(
            msg("a", "секрет один"),
            msg("gone", "секрет", deleted = true),
            msg("b", "другое"),
            msg("c", "Секрет два"),
        )
        assertEquals(listOf("a", "c"), SearchJumpRules.hits(msgs, "секрет"))
        assertTrue(SearchJumpRules.keepPlace("секрет"))
    }

    @Test
    fun wrapsOlderNewerAndLabelsFromOldest() {
        val ids = listOf("a", "b", "c")
        assertEquals("c", SearchJumpRules.latest(ids))
        assertEquals(2, SearchJumpRules.indexOf(ids, null))
        assertEquals("3 из 3", SearchJumpRules.label(ids, null))
        assertEquals("2 из 3", SearchJumpRules.label(ids, "b"))
        assertEquals("a", SearchJumpRules.older(ids, "b"))
        assertEquals("c", SearchJumpRules.older(ids, "a"))
        assertEquals("c", SearchJumpRules.newer(ids, "b"))
        assertEquals("a", SearchJumpRules.newer(ids, "c"))
        assertEquals("b", SearchJumpRules.older(ids, "missing"))
        assertNull(SearchJumpRules.older(emptyList(), "a"))
        assertNull(SearchJumpRules.newer(emptyList(), "a"))
    }

    @Test
    fun singleHitStepsToItself() {
        val ids = listOf("only")
        assertEquals("only", SearchJumpRules.older(ids, "only"))
        assertEquals("only", SearchJumpRules.newer(ids, "only"))
        assertEquals("1 из 1", SearchJumpRules.label(ids, "only"))
    }

    @Test
    fun copyHasNoFcm() {
        assertEquals("Предыдущее", SearchJumpRules.OLDER)
        assertEquals("Следующее", SearchJumpRules.NEWER)
        assertFalse(SearchJumpRules.OLDER.contains("FCM", ignoreCase = true))
        assertFalse(SearchJumpRules.NEWER.contains("FCM", ignoreCase = true))
        assertFalse(SearchJumpRules.NONE.contains("FCM", ignoreCase = true))
    }
}
