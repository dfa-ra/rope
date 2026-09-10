package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.GroupChatUx
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageSearch
import app.rope.android.data.MessageSearchSenderRules
import app.rope.android.data.MessageStatus
import app.rope.android.data.ThreadEmptyRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageSearchSenderRulesTest {
    @Test
    fun chipsAreDistinctThreadSenders() {
        val mine = msg("a", "привет", senderId = "me", outgoing = true, senderName = "Я")
        val ann = msg("b", "ок", senderId = "ann", senderName = "Анна")
        val again = msg("c", "ещё", senderId = "ann", senderName = "Анна")
        val chips = MessageSearchSenderRules.chips(listOf(mine, ann, again), "me")
        assertEquals(listOf("me", "ann"), chips.map { it.id })
        assertEquals(GroupChatUx.YOU, chips[0].label)
        assertEquals("Анна", chips[1].label)
        assertEquals("От кого", MessageSearchSenderRules.CHIP)
        assertNull(MessageSearchSenderRules.toggle("ann", "ann"))
        assertEquals("ann", MessageSearchSenderRules.toggle(null, "ann"))
    }

    @Test
    fun senderFilterCombinesWithTextQuery() {
        val mine = msg("a", "секрет", senderId = "me", outgoing = true)
        val ann = msg("b", "секрет", senderId = "ann", senderName = "Анна")
        val other = msg("c", "привет", senderId = "ann", senderName = "Анна")
        assertTrue(MessageSearchSenderRules.matches(ann, "ann", "me"))
        assertFalse(MessageSearchSenderRules.matches(mine, "ann", "me"))
        assertTrue(MessageSearchSenderRules.matches(mine, null, "me"))
        assertTrue(MessageSearch.matches(ann, "секр") && MessageSearchSenderRules.matches(ann, "ann", "me"))
        assertFalse(MessageSearch.matches(other, "секр") && MessageSearchSenderRules.matches(other, "ann", "me"))
        assertTrue(MessageSearchSenderRules.searching("", "ann"))
        assertFalse(MessageSearchSenderRules.searching("", null))
        assertEquals("Нет сообщений от Анна в этом чате.", MessageSearchSenderRules.emptyBody("Анна"))
        val empty = ThreadEmptyRules.copy("", senderId = "ann", senderLabel = "Анна")
        assertEquals(ThreadEmptyRules.SEARCH_TITLE, empty.title)
        assertEquals("Нет сообщений от Анна в этом чате.", empty.body)
        val withQuery = ThreadEmptyRules.copy("секрет", senderId = "ann", senderLabel = "Анна")
        assertEquals("Нет сообщений по запросу «секрет».", withQuery.body)
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
    }

    private fun msg(
        id: String,
        text: String,
        senderId: String,
        outgoing: Boolean = false,
        senderName: String = "",
        kind: MessageKind = MessageKind.GROUP_TEXT,
    ) = ChatMessage(
        id = id,
        peerDeviceId = "g:1",
        outgoing = outgoing,
        text = text,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        kind = kind,
        senderId = senderId,
        senderName = senderName,
    )
}
