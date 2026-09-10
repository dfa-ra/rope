package app.rope.android

import app.rope.android.data.ChatListMode
import app.rope.android.data.ChatListRules
import app.rope.android.data.ChatMessage
import app.rope.android.data.ChatPrefs
import app.rope.android.data.Conversation
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageStatus
import app.rope.android.data.PinOrderRules
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinOrderRulesTest {
    private val saved = conv(SavedMessagesRules.ID, title = SavedMessagesRules.TITLE, pinned = true)
    private val anna = conv("aaa111", title = "Анна", pinned = true, lastMs = 9_000L)
    private val boris = conv("bbb222", title = "Борис", pinned = true, lastMs = 8_000L)
    private val idle = conv("ccc333", title = "Катя", lastMs = 20_000L)

    @Test
    fun inheritStoreAndKvKey() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("pinRank", PinOrderRules.KV)
        assertEquals("Выше", PinOrderRules.MOVE_UP)
        assertEquals("Ниже", PinOrderRules.MOVE_DOWN)
        assertFalse(ChatPrefs(pinned = true).toJson().contains("pinRank"))
        assertFalse(ChatPrefs(pinned = true).toJson().contains("pin_rank"))
    }

    @Test
    fun jsonRoundTripAndBlankParse() {
        val ranks = mapOf(anna.id to 0, boris.id to 1, SavedMessagesRules.ID to 2)
        val parsed = PinOrderRules.parse(PinOrderRules.toJson(ranks))
        assertEquals(0, parsed[anna.id])
        assertEquals(1, parsed[boris.id])
        assertEquals(2, parsed[SavedMessagesRules.ID])
        assertTrue(PinOrderRules.parse(null).isEmpty())
        assertTrue(PinOrderRules.parse("  ").isEmpty())
        assertTrue(PinOrderRules.parse("{").isEmpty())
        assertEquals(PinOrderRules.UNRANKED, PinOrderRules.rankOf("missing", ranks))
        assertEquals(0, PinOrderRules.rankOf(anna.id, ranks))
    }

    @Test
    fun moveUpDownAndBounds() {
        val ids = listOf(saved.id, anna.id, boris.id)
        assertTrue(PinOrderRules.visible(2, forwarding = false, searching = false))
        assertFalse(PinOrderRules.visible(1, forwarding = false, searching = false))
        assertFalse(PinOrderRules.visible(3, forwarding = true, searching = false))
        assertFalse(PinOrderRules.visible(3, forwarding = false, searching = true))
        assertFalse(PinOrderRules.canMoveUp(ids, saved.id))
        assertTrue(PinOrderRules.canMoveDown(ids, saved.id))
        assertTrue(PinOrderRules.canMoveUp(ids, anna.id))
        assertTrue(PinOrderRules.canMoveDown(ids, anna.id))
        assertTrue(PinOrderRules.canMoveUp(ids, boris.id))
        assertFalse(PinOrderRules.canMoveDown(ids, boris.id))
        assertEquals(listOf(anna.id, saved.id, boris.id), PinOrderRules.move(ids, anna.id, -1))
        assertEquals(listOf(saved.id, boris.id, anna.id), PinOrderRules.move(ids, anna.id, 1))
        assertEquals(ids, PinOrderRules.move(ids, saved.id, -1))
        assertEquals(ids, PinOrderRules.move(ids, boris.id, 1))
        assertEquals(ids, PinOrderRules.move(ids, idle.id, -1))
        assertEquals(mapOf(saved.id to 0, anna.id to 1, boris.id to 2), PinOrderRules.ranksOf(ids))
    }

    @Test
    fun pinUnpinTouchRankOnlyAfterCustomOrder() {
        assertTrue(PinOrderRules.afterPin(emptyMap(), anna.id).isEmpty())
        val custom = mapOf(saved.id to 0, anna.id to 1)
        val afterNew = PinOrderRules.afterPin(custom, boris.id)
        assertEquals(-1, afterNew[boris.id])
        assertEquals(0, afterNew[saved.id])
        assertEquals(custom, PinOrderRules.afterPin(custom, anna.id))
        assertEquals(mapOf(saved.id to 0), PinOrderRules.afterUnpin(custom, anna.id))
        assertEquals(emptyMap<String, Int>(), PinOrderRules.afterUnpin(emptyMap(), anna.id))
    }

    @Test
    fun unrankedPinsKeepSavedFirstThenLastMessage() {
        val rows = ChatListRules.rows(listOf(idle, anna, saved), "", ChatListMode.ALL)
        assertEquals(listOf(saved.id, anna.id, idle.id), rows.map { it.id })
        assertTrue(ChatListRules.compare(saved, anna) < 0)
    }

    @Test
    fun customPinRankBeatsLastMessageAndCanLiftPeerOverSaved() {
        val peerFirst = anna.copy(pinRank = 0)
        val savedSecond = saved.copy(pinRank = 1)
        val later = boris.copy(pinRank = 2)
        val rows = ChatListRules.rows(listOf(later, savedSecond, peerFirst, idle), "", ChatListMode.ALL)
        assertEquals(listOf(anna.id, saved.id, boris.id, idle.id), rows.map { it.id })
        assertEquals(listOf(anna.id, saved.id, boris.id), ChatListRules.pinnedBlock(rows, "").map { it.id })
        val searching = ChatListRules.rows(listOf(later, savedSecond, peerFirst), "анн")
        assertEquals(listOf(anna.id), searching.map { it.id })
    }

    private fun conv(
        id: String,
        title: String,
        pinned: Boolean = false,
        lastMs: Long? = null,
    ): Conversation = Conversation(
        id = id,
        title = title,
        subtitle = "",
        isGroup = false,
        online = false,
        last = lastMs?.let {
            ChatMessage(
                id = "m$it",
                peerDeviceId = id,
                outgoing = false,
                text = "hi",
                status = MessageStatus.DELIVERED_TO_DEVICE,
                timestampMs = it,
            )
        },
        pinned = pinned,
    )
}
