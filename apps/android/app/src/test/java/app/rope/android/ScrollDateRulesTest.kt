package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.ChatThreadItem
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageStatus
import app.rope.android.data.ScrollDateRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScrollDateRulesTest {
    private val yesterday = ChatThreadItem.Day("2026-09-08", "Вчера")
    private val today = ChatThreadItem.Day("2026-09-09", "Сегодня")
    private val items = listOf(
        yesterday,
        bubble("a"),
        today,
        bubble("b"),
        ChatThreadItem.Unread,
        bubble("c"),
    )

    @Test
    fun hidesWhenDayChipIsAtTop() {
        assertNull(ScrollDateRules.stickyLabel(items, 0))
        assertNull(ScrollDateRules.stickyLabel(items, 2))
        assertEquals("Дата", ScrollDateRules.CHIP)
    }

    @Test
    fun usesNearestDayOnceInlineChipScrollsOff() {
        assertEquals("Вчера", ScrollDateRules.stickyLabel(items, 1))
        assertEquals("Сегодня", ScrollDateRules.stickyLabel(items, 3))
        assertEquals("Сегодня", ScrollDateRules.stickyLabel(items, 4))
        assertEquals("Сегодня", ScrollDateRules.stickyLabel(items, 5))
    }

    @Test
    fun emptyAndOobAreHidden() {
        assertNull(ScrollDateRules.stickyLabel(emptyList(), 0))
        assertNull(ScrollDateRules.stickyLabel(items, -1))
        assertEquals("Сегодня", ScrollDateRules.stickyLabel(items, 99))
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
    }

    private fun bubble(id: String) = ChatThreadItem.Bubble(
        ChatMessage(
            id = id,
            peerDeviceId = "p",
            outgoing = false,
            text = id,
            status = MessageStatus.DELIVERED_TO_DEVICE,
            timestampMs = 1L,
        ),
    )
}
