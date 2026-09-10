package app.rope.android

import app.rope.android.data.ChatIds
import app.rope.android.data.ChatPrefs
import app.rope.android.data.LocalStore
import app.rope.android.data.LpOffRules
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LpOffRulesTest {
    @Test
    fun defaultOnAndSavedHides() {
        assertEquals("Предпросмотр ссылок", LpOffRules.TITLE)
        assertFalse(LpOffRules.HINT.contains('\n'))
        assertFalse(LpOffRules.HINT.contains('\r'))
        assertFalse(LpOffRules.applies(null))
        assertFalse(LpOffRules.applies(""))
        assertFalse(LpOffRules.applies(SavedMessagesRules.ID))
        assertTrue(LpOffRules.applies("peer-1"))
        assertTrue(LpOffRules.applies(ChatIds.group("g1")))
        assertTrue(LpOffRules.effective(SavedMessagesRules.ID, prefsOn = false))
        assertFalse(LpOffRules.effective("peer-1", prefsOn = false))
        assertTrue(LpOffRules.effective("peer-1", prefsOn = true))
        assertTrue(LpOffRules.enabled(global = true, chatOn = true))
        assertFalse(LpOffRules.enabled(global = true, chatOn = false))
        assertFalse(LpOffRules.enabled(global = false, chatOn = true))
        assertTrue(ChatPrefs().linkPreviews)
        assertTrue(ChatPrefs.parse(null).linkPreviews)
        assertTrue(ChatPrefs.parse("{}").linkPreviews)
        assertTrue(ChatPrefs.parse("""{"pinned":true}""").linkPreviews)
        assertFalse(ChatPrefs.parse("""{"link_previews":false}""").linkPreviews)
        assertTrue(ChatPrefs.parse("""{"link_previews":true}""").linkPreviews)
        val round = ChatPrefs.parse(ChatPrefs(linkPreviews = false).toJson())
        assertFalse(round.linkPreviews)
        assertTrue(LpOffRules.parsePref(hasKey = false, value = false))
        assertFalse(LpOffRules.parsePref(hasKey = true, value = false))
        assertEquals(6, LocalStore.VERSION)
    }
}
