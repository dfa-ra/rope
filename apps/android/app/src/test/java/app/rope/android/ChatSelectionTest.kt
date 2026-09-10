package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.ChatSelection
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatSelectionTest {
    @Test
    fun titleAndSelectAllCopy() {
        assertEquals("Выбрано 1", ChatSelection.title(1))
        assertEquals("Выбрано 3", ChatSelection.title(3))
        assertEquals("Выбрано 0", ChatSelection.title(0))
        assertEquals("Выбрать все", ChatSelection.SELECT_ALL)
        assertEquals("Снять всё", ChatSelection.CLEAR_ALL)
    }

    @Test
    fun canSelectLiveNotDeleted() {
        val live = msg("a", "hi")
        assertTrue(ChatSelection.canSelect(live))
        assertFalse(ChatSelection.canSelect(live.copy(deleted = true)))
    }

    @Test
    fun selectableIdsSkipDeleted() {
        val a = msg("a", "one")
        val gone = msg("b", "two").copy(deleted = true)
        val c = msg("c", "three")
        assertEquals(setOf("a", "c"), ChatSelection.selectableIds(listOf(a, gone, c)))
        assertTrue(ChatSelection.selectableIds(emptyList()).isEmpty())
        assertFalse(ChatSelection.showsAction(listOf(gone)))
        assertTrue(ChatSelection.showsAction(listOf(a)))
    }

    @Test
    fun nextIdsFillsThenClears() {
        val a = msg("a", "one")
        val gone = msg("b", "two").copy(deleted = true)
        val c = msg("c", "three")
        val thread = listOf(a, gone, c)
        assertEquals(setOf("a", "c"), ChatSelection.nextIds(setOf("a"), thread))
        assertEquals(setOf("a", "c"), ChatSelection.nextIds(emptySet(), thread))
        assertEquals(emptySet<String>(), ChatSelection.nextIds(setOf("a", "c"), thread))
        assertEquals(emptySet<String>(), ChatSelection.nextIds(setOf("a", "b", "c"), thread))
        assertTrue(ChatSelection.allSelected(setOf("a", "c"), thread))
        assertFalse(ChatSelection.allSelected(setOf("a"), thread))
        assertFalse(ChatSelection.allSelected(emptySet(), thread))
        assertEquals(ChatSelection.SELECT_ALL, ChatSelection.actionLabel(setOf("a"), thread))
        assertEquals(ChatSelection.CLEAR_ALL, ChatSelection.actionLabel(setOf("a", "c"), thread))
    }

    private fun msg(id: String, text: String) = ChatMessage(
        id = id,
        peerDeviceId = "p",
        outgoing = false,
        text = text,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        kind = MessageKind.TEXT,
    )
}
