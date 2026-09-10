package app.rope.android

import app.rope.android.data.CallLink
import app.rope.android.data.ChatListMode
import app.rope.android.data.ChatMessage
import app.rope.android.data.Conversation
import app.rope.android.data.IncomingCallRules
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.VideoCallRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomingCallRulesTest {
    private fun call(
        id: String,
        text: String,
        outgoing: Boolean,
        deleted: Boolean = false,
        kind: MessageKind = MessageKind.CALL,
    ) = ChatMessage(
        id = id,
        peerDeviceId = "p",
        outgoing = outgoing,
        text = text,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        kind = kind,
        deleted = deleted,
    )

    private fun conv(
        id: String,
        last: ChatMessage?,
        archived: Boolean = false,
        title: String = id,
    ) = Conversation(
        id = id,
        title = title,
        subtitle = last?.preview().orEmpty(),
        isGroup = false,
        online = false,
        last = last,
        archived = archived,
    )

    @Test
    fun incomingRecordAndMissedAreIncoming() {
        val audioIn = call("1", VideoCallRules.recordLabel(false, outgoing = false), outgoing = false)
        val videoIn = call("2", VideoCallRules.recordLabel(true, outgoing = false), outgoing = false)
        val missed = call("3", CallLink.ringTimeoutDetail(false), outgoing = false)
        assertEquals("пропущен", CallLink.ringTimeoutDetail(false))
        assertTrue(IncomingCallRules.isIncoming(audioIn))
        assertTrue(IncomingCallRules.isIncoming(videoIn))
        assertTrue(IncomingCallRules.isIncoming(missed))
        assertTrue(IncomingCallRules.isIncoming(missed.copy(outgoing = true)))
    }

    @Test
    fun outgoingAndNoAnswerStayOnAll() {
        val out = call("1", VideoCallRules.recordLabel(false, outgoing = true), outgoing = true)
        val noAnswer = call("2", CallLink.ringTimeoutDetail(true), outgoing = true)
        val text = call("3", "привет", outgoing = false, kind = MessageKind.TEXT)
        val gone = call("4", VideoCallRules.recordLabel(false, outgoing = false), outgoing = false, deleted = true)
        assertEquals("абонент не ответил", CallLink.ringTimeoutDetail(true))
        assertFalse(IncomingCallRules.isIncoming(out))
        assertFalse(IncomingCallRules.isIncoming(noAnswer))
        assertFalse(IncomingCallRules.isIncoming(text))
        assertFalse(IncomingCallRules.isIncoming(gone))
        assertFalse(IncomingCallRules.isIncoming(null))
    }

    @Test
    fun recentFiltersIncomingAndDropsArchived() {
        val missed = conv("m", call("1", "Входящий звонок", outgoing = false))
        val hangup = conv("h", call("2", "пропущен", outgoing = false))
        val out = conv("o", call("3", "Исходящий звонок", outgoing = true))
        val archived = conv(
            "a",
            call("4", "Входящий звонок", outgoing = false),
            archived = true,
        )
        val all = listOf(missed, hangup, out, archived)
        assertEquals(listOf("m", "h", "o"), IncomingCallRules.recent(all, incomingOnly = false).map { it.id })
        assertEquals(listOf("m", "h"), IncomingCallRules.recent(all, incomingOnly = true).map { it.id })
        assertTrue(IncomingCallRules.showPeople(incomingOnly = false))
        assertFalse(IncomingCallRules.showPeople(incomingOnly = true))
    }

    @Test
    fun chipsAndEmptyCopy() {
        assertEquals("Все", IncomingCallRules.CHIP_ALL)
        assertEquals("Входящие", IncomingCallRules.CHIP_INCOMING)
        assertEquals("Нет входящих", IncomingCallRules.emptyTitle(true))
        assertEquals("Нет входящих звонков.", IncomingCallRules.emptyBody(true, "owner"))
        assertNull(IncomingCallRules.emptyAction(true, "owner"))
        assertEquals("Звонков ещё не было", IncomingCallRules.emptyTitle(false))
        assertEquals("Нажмите трубку в чате.", IncomingCallRules.emptyBody(false, "guest"))
        assertEquals("Пригласить", IncomingCallRules.emptyAction(false, "owner"))
        assertNull(IncomingCallRules.emptyAction(false, "guest"))
        assertFalse(IncomingCallRules.enabledFromKv(null))
        assertFalse(IncomingCallRules.enabledFromKv("0"))
        assertTrue(IncomingCallRules.enabledFromKv("1"))
        assertEquals(6, LocalStore.VERSION)
        assertEquals(ChatListMode.CALLS, NavRules.listMode(Screen.Calls))
    }
}
