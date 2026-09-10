package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.Reaction
import app.rope.android.data.ReactionWhoRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReactionWhoRulesTest {
    @Test
    fun namesUseDisplayOrFallback() {
        val people = listOf(
            Reaction("👍", "d1", "Анна"),
            Reaction("👍", "d2", "  "),
            Reaction("👍", "d3", ""),
        )
        assertTrue(ReactionWhoRules.canShow(people))
        assertEquals(listOf("Анна", "Участник", "Участник"), ReactionWhoRules.names(people))
        assertEquals("👍 · 3", ReactionWhoRules.title("👍", 3))
        assertEquals("🙂 · 0", ReactionWhoRules.title("  ", 0))
        assertFalse(ReactionWhoRules.canShow(emptyList()))
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
    }
}
