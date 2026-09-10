package app.rope.android

import app.rope.android.data.ChatListRules
import app.rope.android.data.ChatPrefs
import app.rope.android.data.Conversation
import app.rope.android.data.LocalStore
import app.rope.android.data.SavedMessagesRules
import app.rope.android.data.UnpinAllRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnpinAllRulesTest {
    private val anna = conv("aaa111", title = "Анна", pinned = true)
    private val boris = conv("bbb222", title = "Борис", pinned = true)
    private val idle = conv("ccc333", title = "Катя")
    private val saved = conv(SavedMessagesRules.ID, title = SavedMessagesRules.TITLE, pinned = true)

    @Test
    fun unpinAllNeedsTwoPinnedAndKeepsSavedInTheSet() {
        assertEquals(emptyList<String>(), UnpinAllRules.ids(listOf(idle)))
        assertEquals(listOf(anna.id, boris.id), UnpinAllRules.ids(listOf(anna, boris, idle)))
        assertTrue(UnpinAllRules.ids(listOf(anna, saved)).contains(SavedMessagesRules.ID))
        assertFalse(UnpinAllRules.visible(1, forwarding = false, searching = false))
        assertTrue(UnpinAllRules.visible(2, forwarding = false, searching = false))
        assertFalse(UnpinAllRules.visible(2, forwarding = true, searching = false))
        assertFalse(UnpinAllRules.visible(2, forwarding = false, searching = true))
        assertFalse(UnpinAllRules.showInRowMenu(pinned = true, pinnedCount = 1))
        assertTrue(UnpinAllRules.showInRowMenu(pinned = true, pinnedCount = 2))
        assertFalse(UnpinAllRules.showInRowMenu(pinned = false, pinnedCount = 3))
        assertFalse(UnpinAllRules.prefsAfter(ChatPrefs(pinned = true, muted = true)).pinned)
        assertTrue(UnpinAllRules.prefsAfter(ChatPrefs(pinned = true, muted = true)).muted)
        assertTrue(ChatListRules.searching("анн"))
        assertEquals(UnpinAllRules.ACTION, "Открепить все")
        assertEquals(UnpinAllRules.CONFIRM, "Точно открепить все")
        assertEquals(6, LocalStore.VERSION)
    }

    private fun conv(
        id: String,
        title: String,
        pinned: Boolean = false,
    ): Conversation = Conversation(
        id = id,
        title = title,
        subtitle = "",
        isGroup = false,
        online = false,
        last = null,
        pinned = pinned,
    )
}
