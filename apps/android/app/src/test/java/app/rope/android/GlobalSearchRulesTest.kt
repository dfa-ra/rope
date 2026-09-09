package app.rope.android

import app.rope.android.data.ChatIds
import app.rope.android.data.ChatListMode
import app.rope.android.data.ChatMessage
import app.rope.android.data.ChatRouting
import app.rope.android.data.GlobalSearchRules
import app.rope.android.data.MessageStatus
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalSearchRulesTest {
    @Test
    fun shortQueryDoesNotSearch() {
        assertFalse(GlobalSearchRules.shouldSearch("а", forwarding = false, mode = ChatListMode.ALL))
        assertTrue(GlobalSearchRules.shouldSearch("ан", forwarding = false, mode = ChatListMode.ALL))
        assertFalse(GlobalSearchRules.shouldSearch("анна", forwarding = true, mode = ChatListMode.ALL))
        assertFalse(GlobalSearchRules.shouldSearch("анна", forwarding = false, mode = ChatListMode.GROUPS))
    }

    @Test
    fun skipsDeletedAndBlankIds() {
        val ok = msg("m1", "dev", "привет Анна")
        assertTrue(GlobalSearchRules.matches(ok, "анна"))
        assertFalse(GlobalSearchRules.matches(ok.copy(deleted = true), "анна"))
        assertFalse(GlobalSearchRules.matches(ok.copy(id = ""), "анна"))
        assertFalse(GlobalSearchRules.matches(ok.copy(peerDeviceId = ""), "анна"))
    }

    @Test
    fun newestFirstCappedAndTitled() {
        val older = msg("m1", "dev-a", "секретный план", ts = 10L)
        val newer = msg("m2", "dev-b", "ещё секрет", ts = 200L)
        val filler = (1..50).map { msg("x$it", "dev-c", "секрет $it", ts = 20L + it) }
        val hits = GlobalSearchRules.hits(
            listOf(older, newer) + filler,
            mapOf("dev-a" to "Анна", "dev-b" to "Борис"),
            "секрет",
        )
        assertEquals(GlobalSearchRules.MAX_HITS, hits.size)
        assertEquals("m2", hits.first().messageId)
        assertEquals("Борис", hits.first().title)
        assertTrue(hits.none { it.messageId == "m1" })
        assertTrue(hits.first().timestampMs >= hits.last().timestampMs)
    }

    @Test
    fun snippetCollapsesWhitespace() {
        val hit = GlobalSearchRules.hits(
            listOf(msg("m", "dev", "строка\n  два")),
            emptyMap(),
            "строка",
        ).single()
        assertEquals("строка два", hit.snippet)
        assertEquals("dev", hit.title)
    }

    @Test
    fun skipsLeftGroupThreads() {
        val live = ChatIds.group("live-uuid")
        val left = ChatIds.group("left-uuid")
        val liveMsg = msg("m1", live, "секрет в живой", ts = 30L)
        val leftMsg = msg("m2", left, "секрет в ушедшей", ts = 99L)
        val leftoverDm = msg("m3", "peer-gone", "секрет лично", ts = 50L)
        val hits = GlobalSearchRules.hits(
            listOf(liveMsg, leftMsg, leftoverDm),
            mapOf(live to "Команда", left to "Старая"),
            "секрет",
            openableChatIds = setOf(live),
        )
        assertTrue(hits.none { it.chatId == left })
        assertEquals(listOf("m3", "m1"), hits.map { it.messageId })
        assertFalse(GlobalSearchRules.canOpen(left, setOf(live)))
        assertTrue(GlobalSearchRules.canOpen(live, setOf(live)))
        assertTrue(GlobalSearchRules.canOpen("peer-gone", emptySet()))
        assertTrue(GlobalSearchRules.canOpen(SavedMessagesRules.ID, emptySet()))
        assertFalse(GlobalSearchRules.canOpen("", emptySet()))
        assertFalse(ChatRouting.showLeftoverThread(left))
    }

    private fun msg(id: String, peer: String, text: String, ts: Long = 1L) = ChatMessage(
        id = id,
        peerDeviceId = peer,
        outgoing = false,
        text = text,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = ts,
    )
}
