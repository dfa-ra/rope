package app.rope.android

import app.rope.android.data.ArchiveRules
import app.rope.android.data.ChatIds
import app.rope.android.data.Conversation
import app.rope.android.data.LocalStore
import app.rope.android.data.PinChatRules
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinChatRulesTest {
    private val anna = conv("aaa111", title = "Анна")
    private val pinnedAnna = anna.copy(pinned = true)
    private val archivedAnna = anna.copy(archived = true, pinned = false)
    private val group = conv(ChatIds.group("g-uuid"), title = "Команда", group = true)
    private val saved = conv(SavedMessagesRules.ID, title = SavedMessagesRules.TITLE, pinned = true)

    @Test
    fun profilePinLooksUpRowAndSkipsArchived() {
        assertFalse(PinChatRules.canPin(null, emptyList()))
        assertFalse(PinChatRules.canPin("", emptyList()))
        assertTrue(PinChatRules.canPin(anna.id, emptyList()))
        assertTrue(PinChatRules.canPin(anna.id, listOf(anna)))
        assertTrue(PinChatRules.canPin(group.id, listOf(group)))
        assertFalse(PinChatRules.canPin(archivedAnna.id, listOf(archivedAnna)))
        assertFalse(PinChatRules.isPinned(listOf(anna), anna.id))
        assertTrue(PinChatRules.isPinned(listOf(pinnedAnna), pinnedAnna.id))
        assertFalse(PinChatRules.isPinned(listOf(pinnedAnna), null))
        assertEquals(PinChatRules.PIN, PinChatRules.profileAction(false))
        assertEquals(PinChatRules.UNPIN, PinChatRules.profileAction(true))
        assertTrue(ArchiveRules.canPin(saved))
        assertEquals(6, LocalStore.VERSION)
    }

    private fun conv(
        id: String,
        title: String,
        group: Boolean = false,
        pinned: Boolean = false,
    ): Conversation = Conversation(
        id = id,
        title = title,
        subtitle = "",
        isGroup = group,
        online = false,
        last = null,
        pinned = pinned,
    )
}
