package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.ChatSelection
import app.rope.android.data.GroupChatUx
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatSelectionTest {
    @Test
    fun menuCopyAndTitle() {
        assertEquals("Выбрать", ChatSelection.MENU)
        assertEquals("Копировать", ChatSelection.COPY)
        assertEquals("Выбрано 2", ChatSelection.title(2))
    }

    @Test
    fun canSelectLiveNotDeleted() {
        val live = msg("a", "hi")
        assertTrue(ChatSelection.canSelect(live))
        assertFalse(ChatSelection.canSelect(live.copy(deleted = true)))
    }

    @Test
    fun copyTextSingleIsPlain() {
        val a = msg("a", "привет")
        assertEquals("привет", ChatSelection.copyText(listOf(a)))
        assertEquals("", ChatSelection.copyText(listOf(a.copy(deleted = true))))
        assertEquals("", ChatSelection.copyText(emptyList()))
    }

    @Test
    fun copyTextManyPrefixesSender() {
        val incoming = msg("a", "привет", outgoing = false, senderName = "Анна")
        val mine = msg("b", "ок", outgoing = true, senderName = "Я")
        val packed = ChatSelection.copyText(listOf(incoming, mine))
        assertEquals("Анна: привет\n\n${GroupChatUx.YOU}: ок", packed)
        val nameless = msg("c", "хм", outgoing = false, senderName = "")
        assertEquals(
            "сообщение: хм\n\nАнна: привет",
            ChatSelection.copyText(listOf(nameless, incoming)),
        )
    }

    private fun msg(
        id: String,
        text: String,
        outgoing: Boolean = false,
        senderName: String = "",
    ) = ChatMessage(
        id = id,
        peerDeviceId = "p",
        outgoing = outgoing,
        text = text,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        kind = MessageKind.TEXT,
        senderName = senderName,
    )
}
