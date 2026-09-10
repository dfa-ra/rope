package app.rope.android

import app.rope.android.data.ChatPrefs
import app.rope.android.data.Conversation
import app.rope.android.data.LocalStore
import app.rope.android.data.UnmuteAllRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnmuteAllRulesTest {
    @Test
    fun unmuteClearsOnlyMuted() {
        val muted = ChatPrefs(muted = true, pinned = true, unread = 2)
        val quiet = ChatPrefs(muted = false, archived = true)
        val after = UnmuteAllRules.unmute(muted)
        assertFalse(after.muted)
        assertTrue(after.pinned)
        assertEquals(2, after.unread)
        assertEquals(quiet, UnmuteAllRules.unmute(quiet))
        val all = mapOf("a" to muted, "b" to quiet)
        assertEquals(1, UnmuteAllRules.mutedCount(all))
        assertTrue(UnmuteAllRules.anyMuted(1))
        assertFalse(UnmuteAllRules.anyMuted(0))
        val cleared = UnmuteAllRules.unmuted(all)
        assertFalse(cleared.getValue("a").muted)
        assertFalse(cleared.getValue("b").muted)
        assertTrue(cleared.getValue("b").archived)
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun copyAndConversationCount() {
        val rows = listOf(
            Conversation("1", "Анна", "привет", false, true, null, muted = true),
            Conversation("2", "Боб", "ок", false, false, null, muted = false),
            Conversation("3", "Кира", "хм", false, false, null, muted = true),
        )
        assertEquals(2, UnmuteAllRules.mutedCount(rows))
        assertEquals("Включить звук во всех", UnmuteAllRules.actionLabel())
        assertEquals("Звук включён в 1 чате", UnmuteAllRules.notice(1))
        assertEquals("Звук включён в 3 чатах", UnmuteAllRules.notice(3))
        assertEquals("", UnmuteAllRules.notice(0))
        assertTrue(UnmuteAllRules.hint().contains("списке"))
        assertEquals(6, LocalStore.VERSION)
    }
}
