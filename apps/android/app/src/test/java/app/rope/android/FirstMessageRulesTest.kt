package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.FirstMessageRules
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstMessageRulesTest {
    @Test
    fun oldestNonDeletedWinsAndSkipsEmpty() {
        val older = msg("a", 10L)
        val newer = msg("b", 20L)
        val deleted = msg("c", 1L, deleted = true)
        val sameTsEarlierId = msg("d", 10L)
        assertNull(FirstMessageRules.id(emptyList()))
        assertNull(FirstMessageRules.id(listOf(deleted)))
        assertFalse(FirstMessageRules.visible(FirstMessageRules.id(emptyList())))
        assertEquals("a", FirstMessageRules.id(listOf(newer, older, deleted)))
        assertEquals("a", FirstMessageRules.id(listOf(sameTsEarlierId, older)))
        assertTrue(FirstMessageRules.visible("a"))
        assertEquals("В начало", FirstMessageRules.ACTION)
        assertEquals(6, LocalStore.VERSION)
    }

    private fun msg(id: String, ts: Long, deleted: Boolean = false) = ChatMessage(
        id = id,
        peerDeviceId = "peer",
        outgoing = false,
        text = "hi",
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = ts,
        kind = MessageKind.TEXT,
        deleted = deleted,
    )
}
