package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.ChatThreadItem
import app.rope.android.data.DateSeparatorRules
import app.rope.android.data.GroupChatUx
import app.rope.android.data.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DateSeparatorRulesTest {
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")
    private val now = utcMs(2026, Calendar.SEPTEMBER, 9, 15)

    @Test
    fun labelsMatchTelegramBuckets() {
        assertEquals("Сегодня", DateSeparatorRules.label(utcMs(2026, Calendar.SEPTEMBER, 9, 1), now, utc))
        assertEquals("Вчера", DateSeparatorRules.label(utcMs(2026, Calendar.SEPTEMBER, 8, 23), now, utc))
        assertEquals("понедельник", DateSeparatorRules.label(utcMs(2026, Calendar.SEPTEMBER, 7, 12), now, utc))
        assertEquals("четверг", DateSeparatorRules.label(utcMs(2026, Calendar.SEPTEMBER, 3, 12), now, utc))
        assertEquals("1 августа", DateSeparatorRules.label(utcMs(2026, Calendar.AUGUST, 1, 12), now, utc))
        assertEquals("9 сентября 2025", DateSeparatorRules.label(utcMs(2025, Calendar.SEPTEMBER, 9, 12), now, utc))
    }

    @Test
    fun sevenDaysAgoIsADateNotAWeekday() {
        assertEquals("2 сентября", DateSeparatorRules.label(utcMs(2026, Calendar.SEPTEMBER, 2, 12), now, utc))
        assertEquals(7, DateSeparatorRules.daysAgo(utcMs(2026, Calendar.SEPTEMBER, 2, 12), now, utc))
    }

    @Test
    fun shouldShowOnDayChangeOnly() {
        val first = msg("a", utcMs(2026, Calendar.SEPTEMBER, 8, 10))
        val same = msg("b", utcMs(2026, Calendar.SEPTEMBER, 8, 22))
        val next = msg("c", utcMs(2026, Calendar.SEPTEMBER, 9, 0))
        assertTrue(DateSeparatorRules.shouldShow(null, first, utc))
        assertFalse(DateSeparatorRules.shouldShow(first, same, utc))
        assertTrue(DateSeparatorRules.shouldShow(same, next, utc))
        assertTrue(DateSeparatorRules.sameDay(first.timestampMs, same.timestampMs, utc))
        assertFalse(DateSeparatorRules.sameDay(same.timestampMs, next.timestampMs, utc))
    }

    @Test
    fun itemsInsertChipPerDay() {
        val a = msg("a", utcMs(2026, Calendar.SEPTEMBER, 8, 10))
        val b = msg("b", utcMs(2026, Calendar.SEPTEMBER, 8, 11))
        val c = msg("c", utcMs(2026, Calendar.SEPTEMBER, 9, 9))
        val items = DateSeparatorRules.items(listOf(a, b, c), now, utc)
        assertEquals(5, items.size)
        val d0 = items[0] as ChatThreadItem.Day
        val d1 = items[3] as ChatThreadItem.Day
        assertEquals("Вчера", d0.label)
        assertEquals("2026-09-08", d0.dayKey)
        assertEquals("a", (items[1] as ChatThreadItem.Bubble).msg.id)
        assertEquals("b", (items[2] as ChatThreadItem.Bubble).msg.id)
        assertEquals("Сегодня", d1.label)
        assertEquals("c", (items[4] as ChatThreadItem.Bubble).msg.id)
        assertEquals(4, DateSeparatorRules.indexOfMessage(items, "c"))
        assertEquals(emptyList<ChatThreadItem>(), DateSeparatorRules.items(emptyList(), now, utc))
        val one = DateSeparatorRules.items(listOf(c), now, utc)
        assertEquals(2, one.size)
        assertEquals("Сегодня", (one[0] as ChatThreadItem.Day).label)
    }

    @Test
    fun clusterBreaksAcrossCalendarDays() {
        val prevTz = TimeZone.getDefault()
        TimeZone.setDefault(utc)
        try {
            val t0 = utcMs(2026, Calendar.SEPTEMBER, 8, 23, 58)
            val t1 = utcMs(2026, Calendar.SEPTEMBER, 9, 0, 2)
            val a = msg("a", t0, senderId = "d2")
            val b = msg("b", t1, senderId = "d2")
            assertTrue(kotlin.math.abs(t1 - t0) < GroupChatUx.CLUSTER_GAP_MS)
            assertFalse(GroupChatUx.sameCluster(a, b))
            assertTrue(GroupChatUx.firstInCluster(listOf(a, b), 1))
        } finally {
            TimeZone.setDefault(prevTz)
        }
    }

    private fun msg(
        id: String,
        ts: Long,
        senderId: String = "d1",
    ) = ChatMessage(
        id = id,
        peerDeviceId = "p",
        outgoing = false,
        text = id,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = ts,
        senderId = senderId,
        senderName = "Аня",
    )

    private fun utcMs(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
