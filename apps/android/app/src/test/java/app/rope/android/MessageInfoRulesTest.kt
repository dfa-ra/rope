package app.rope.android

import app.rope.android.data.ChatActions
import app.rope.android.data.ChatMessage
import app.rope.android.data.MessageInfoRules
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class MessageInfoRulesTest {
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    private fun msg(
        outgoing: Boolean,
        status: MessageStatus = MessageStatus.DELIVERED_TO_DEVICE,
        deleted: Boolean = false,
        edited: Boolean = false,
        ts: Long = 1_704_110_400_000L, // 2024-01-01 12:00:00 UTC
    ) = ChatMessage(
        id = "m1",
        peerDeviceId = "peer",
        outgoing = outgoing,
        text = "привет",
        status = status,
        timestampMs = ts,
        kind = MessageKind.TEXT,
        edited = edited,
        deleted = deleted,
    )

    @Test
    fun hidesDeleted() {
        assertTrue(MessageInfoRules.canShow(msg(outgoing = true)))
        assertTrue(MessageInfoRules.canShow(msg(outgoing = false)))
        assertFalse(MessageInfoRules.canShow(msg(outgoing = true, deleted = true)))
        assertTrue(MessageInfoRules.rows(msg(outgoing = true, deleted = true)).isEmpty())
    }

    @Test
    fun outgoingShowsStatus() {
        val waiting = MessageInfoRules.rows(msg(outgoing = true, status = MessageStatus.CREATED), utc)
        assertEquals("Ожидает отправки", waiting.single { it.label == "Статус" }.value)
        val sent = MessageInfoRules.rows(msg(outgoing = true, status = MessageStatus.SENT_TO_SERVER), utc)
        assertEquals("Отправлено", sent.single { it.label == "Статус" }.value)
        val delivered = MessageInfoRules.rows(msg(outgoing = true, status = MessageStatus.DELIVERED_TO_DEVICE), utc)
        assertEquals("Доставлено", delivered.single { it.label == "Статус" }.value)
        assertEquals("Информация", MessageInfoRules.LABEL)
    }

    @Test
    fun incomingOmitsStatus() {
        val rows = MessageInfoRules.rows(msg(outgoing = false), utc)
        assertTrue(rows.any { it.label == "Отправлено" })
        assertFalse(rows.any { it.label == "Статус" })
        assertNull(MessageInfoRules.statusCopy(msg(outgoing = false)))
    }

    @Test
    fun editedRowAndClock() {
        val rows = MessageInfoRules.rows(msg(outgoing = true, edited = true), utc)
        assertEquals("да", rows.single { it.label == "Изменено" }.value)
        val stamp = MessageInfoRules.sentAt(1_704_110_400_000L, utc)
        assertTrue(stamp.contains("2024"))
        assertTrue(stamp.contains("12:00"))
        assertTrue(MessageInfoRules.rows(msg(outgoing = false, edited = false), utc).none { it.label == "Изменено" })
        assertTrue(ChatActions.canInfo(msg(outgoing = true)))
        assertFalse(ChatActions.canInfo(msg(outgoing = true, deleted = true)))
    }
}
