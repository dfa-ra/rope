package app.rope.android

import app.rope.android.data.CallLink
import app.rope.android.data.ChatListMode
import app.rope.android.data.ChatMessage
import app.rope.android.data.Conversation
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.MissedCallRules
import app.rope.android.data.VideoCallRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MissedCallRulesTest {
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
    fun incomingRecordAndHangupCopyAreMissed() {
        val audioIn = call("1", VideoCallRules.recordLabel(false, outgoing = false), outgoing = false)
        val videoIn = call("2", VideoCallRules.recordLabel(true, outgoing = false), outgoing = false)
        val hangup = call("3", CallLink.ringTimeoutDetail(false), outgoing = false)
        assertEquals("пропущен", CallLink.ringTimeoutDetail(false))
        assertTrue(MissedCallRules.isMissed(audioIn))
        assertTrue(MissedCallRules.isMissed(videoIn))
        assertTrue(MissedCallRules.isMissed(hangup))
        assertTrue(MissedCallRules.isMissed(hangup.copy(outgoing = true)))
    }

    @Test
    fun outgoingAndNonCallAreNotMissed() {
        val out = call("1", VideoCallRules.recordLabel(false, outgoing = true), outgoing = true)
        val noAnswer = call("2", CallLink.ringTimeoutDetail(true), outgoing = true)
        val text = call("3", "привет", outgoing = false, kind = MessageKind.TEXT)
        val gone = call("4", VideoCallRules.recordLabel(false, outgoing = false), outgoing = false, deleted = true)
        assertFalse(MissedCallRules.isMissed(out))
        assertFalse(MissedCallRules.isMissed(noAnswer))
        assertFalse(MissedCallRules.isMissed(text))
        assertFalse(MissedCallRules.isMissed(gone))
        assertFalse(MissedCallRules.isMissed(null))
        assertEquals("абонент не ответил", CallLink.noAnswerDetail())
    }

    @Test
    fun recentFiltersMissedAndDropsArchived() {
        val missed = conv("m", call("1", "Входящий звонок", outgoing = false))
        val out = conv("o", call("2", "Исходящий звонок", outgoing = true))
        val archived = conv(
            "a",
            call("3", "Входящий звонок", outgoing = false),
            archived = true,
        )
        val all = listOf(missed, out, archived)
        assertEquals(listOf("m", "o"), MissedCallRules.recent(all, missedOnly = false).map { it.id })
        assertEquals(listOf("m"), MissedCallRules.recent(all, missedOnly = true).map { it.id })
        assertTrue(MissedCallRules.showPeople(missedOnly = false))
        assertFalse(MissedCallRules.showPeople(missedOnly = true))
    }

    @Test
    fun chipsAndEmptyCopy() {
        assertEquals("Все", MissedCallRules.CHIP_ALL)
        assertEquals("Пропущенные", MissedCallRules.CHIP_MISSED)
        assertEquals("Нет пропущенных", MissedCallRules.emptyTitle(true))
        assertEquals("Нет входящих без ответа.", MissedCallRules.emptyBody(true, "owner"))
        assertNull(MissedCallRules.emptyAction(true, "owner"))
        assertEquals("Звонков ещё не было", MissedCallRules.emptyTitle(false))
        assertEquals("Нажмите трубку в чате.", MissedCallRules.emptyBody(false, "guest"))
        assertEquals("Пригласить", MissedCallRules.emptyAction(false, "owner"))
        assertNull(MissedCallRules.emptyAction(false, "guest"))
        assertFalse(MissedCallRules.enabledFromKv(null))
        assertFalse(MissedCallRules.enabledFromKv("0"))
        assertTrue(MissedCallRules.enabledFromKv("1"))
        assertEquals(6, LocalStore.VERSION)
        assertEquals(ChatListMode.CALLS, NavRules.listMode(Screen.Calls))
    }
}
