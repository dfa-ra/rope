package app.rope.android

import app.rope.android.data.CallLink
import app.rope.android.data.ChatListMode
import app.rope.android.data.ChatMessage
import app.rope.android.data.Conversation
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.OutgoingCallRules
import app.rope.android.data.VideoCallRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OutgoingCallRulesTest {
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
    fun outgoingRecordAndNoAnswerAreOutgoing() {
        val audioOut = call("1", VideoCallRules.recordLabel(false, outgoing = true), outgoing = true)
        val videoOut = call("2", VideoCallRules.recordLabel(true, outgoing = true), outgoing = true)
        val noAnswer = call("3", CallLink.ringTimeoutDetail(true), outgoing = true)
        val offline = call("4", CallLink.offlineDetail(), outgoing = true)
        assertEquals("абонент не ответил", CallLink.ringTimeoutDetail(true))
        assertTrue(OutgoingCallRules.isOutgoing(audioOut))
        assertTrue(OutgoingCallRules.isOutgoing(videoOut))
        assertTrue(OutgoingCallRules.isOutgoing(noAnswer))
        assertTrue(OutgoingCallRules.isOutgoing(noAnswer.copy(outgoing = false)))
        assertTrue(OutgoingCallRules.isOutgoing(offline))
    }

    @Test
    fun incomingAndMissedStayOnAll() {
        val incoming = call("1", VideoCallRules.recordLabel(false, outgoing = false), outgoing = false)
        val videoIn = call("2", VideoCallRules.recordLabel(true, outgoing = false), outgoing = false)
        val missed = call("3", CallLink.ringTimeoutDetail(false), outgoing = false)
        val text = call("4", "привет", outgoing = true, kind = MessageKind.TEXT)
        val gone = call("5", VideoCallRules.recordLabel(false, outgoing = true), outgoing = true, deleted = true)
        assertEquals("пропущен", CallLink.ringTimeoutDetail(false))
        assertFalse(OutgoingCallRules.isOutgoing(incoming))
        assertFalse(OutgoingCallRules.isOutgoing(videoIn))
        assertFalse(OutgoingCallRules.isOutgoing(missed))
        assertFalse(OutgoingCallRules.isOutgoing(missed.copy(outgoing = true)))
        assertFalse(OutgoingCallRules.isOutgoing(text))
        assertFalse(OutgoingCallRules.isOutgoing(gone))
        assertFalse(OutgoingCallRules.isOutgoing(null))
    }

    @Test
    fun recentFiltersOutgoingAndDropsArchived() {
        val missed = conv("m", call("1", "Входящий звонок", outgoing = false))
        val out = conv("o", call("2", "Исходящий звонок", outgoing = true))
        val noAnswer = conv("n", call("3", "абонент не ответил", outgoing = true))
        val archived = conv(
            "a",
            call("4", "Исходящий звонок", outgoing = true),
            archived = true,
        )
        val all = listOf(missed, out, noAnswer, archived)
        assertEquals(listOf("m", "o", "n"), OutgoingCallRules.recent(all, outgoingOnly = false).map { it.id })
        assertEquals(listOf("o", "n"), OutgoingCallRules.recent(all, outgoingOnly = true).map { it.id })
        assertTrue(OutgoingCallRules.showPeople(outgoingOnly = false))
        assertFalse(OutgoingCallRules.showPeople(outgoingOnly = true))
    }

    @Test
    fun chipsAndEmptyCopy() {
        assertEquals("Все", OutgoingCallRules.CHIP_ALL)
        assertEquals("Исходящие", OutgoingCallRules.CHIP_OUTGOING)
        assertEquals("Нет исходящих", OutgoingCallRules.emptyTitle(true))
        assertEquals("Нет исходящих звонков.", OutgoingCallRules.emptyBody(true, "owner"))
        assertNull(OutgoingCallRules.emptyAction(true, "owner"))
        assertEquals("Звонков ещё не было", OutgoingCallRules.emptyTitle(false))
        assertEquals("Нажмите трубку в чате.", OutgoingCallRules.emptyBody(false, "guest"))
        assertEquals("Пригласить", OutgoingCallRules.emptyAction(false, "owner"))
        assertNull(OutgoingCallRules.emptyAction(false, "guest"))
        assertFalse(OutgoingCallRules.enabledFromKv(null))
        assertFalse(OutgoingCallRules.enabledFromKv("0"))
        assertTrue(OutgoingCallRules.enabledFromKv("1"))
        assertEquals(6, LocalStore.VERSION)
        assertEquals(ChatListMode.CALLS, NavRules.listMode(Screen.Calls))
    }
}
