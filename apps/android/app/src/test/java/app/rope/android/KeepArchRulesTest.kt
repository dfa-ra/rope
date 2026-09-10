package app.rope.android

import app.rope.android.data.ArchiveRules
import app.rope.android.data.ChatPrefs
import app.rope.android.data.KeepArchRules
import app.rope.android.data.LocalStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeepArchRulesTest {
    @Test
    fun kvDefaultsOn() {
        assertTrue(KeepArchRules.enabledFromKv(null))
        assertTrue(KeepArchRules.enabledFromKv(""))
        assertTrue(KeepArchRules.enabledFromKv("1"))
        assertFalse(KeepArchRules.enabledFromKv("0"))
        assertEquals("1", KeepArchRules.persist(true))
        assertEquals("0", KeepArchRules.persist(false))
        assertTrue(KeepArchRules.hint().contains("архив"))
    }

    @Test
    fun incomingKeepsArchivedByDefault() {
        val cur = ChatPrefs(archived = true, unread = 2, pinned = false)
        val after = KeepArchRules.nextPrefs(cur, keepArchived = true)
        assertTrue(after.archived)
        assertEquals(3, after.unread)
        assertFalse(after.pinned)
    }

    @Test
    fun incomingUnarchivesWhenToggleOff() {
        val cur = ChatPrefs(archived = true, unread = 4, muted = true)
        val after = KeepArchRules.nextPrefs(cur, keepArchived = false)
        assertFalse(after.archived)
        assertEquals(5, after.unread)
        assertTrue(after.muted)
        val open = ChatPrefs(archived = false, unread = 0)
        val stayed = KeepArchRules.nextPrefs(open, keepArchived = false)
        assertFalse(stayed.archived)
        assertEquals(1, stayed.unread)
        assertEquals(ArchiveRules.unarchivePrefs(cur.copy(unread = 5)).archived, after.archived)
        assertEquals(6, LocalStore.VERSION)
    }
}
