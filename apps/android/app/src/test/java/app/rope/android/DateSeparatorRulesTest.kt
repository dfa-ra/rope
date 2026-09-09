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
    private val berlin: TimeZone = TimeZone.getTimeZone("Europe/Berlin")
    private val moscow: TimeZone = TimeZone.getTimeZone("Europe/Moscow")
    private val now = zonedMs(utc, 2026, Calendar.SEPTEMBER, 9, 15)

    @Test
    fun labelsMatchTelegramBuckets() {
        assertEquals("Сегодня", DateSeparatorRules.label(zonedMs(utc, 2026, Calendar.SEPTEMBER, 9, 1), now, utc))
        assertEquals("Вчера", DateSeparatorRules.label(zonedMs(utc, 2026, Calendar.SEPTEMBER, 8, 23), now, utc))
        assertEquals("понедельник", DateSeparatorRules.label(zonedMs(utc, 2026, Calendar.SEPTEMBER, 7, 12), now, utc))
        assertEquals("четверг", DateSeparatorRules.label(zonedMs(utc, 2026, Calendar.SEPTEMBER, 3, 12), now, utc))
        assertEquals("1 августа", DateSeparatorRules.label(zonedMs(utc, 2026, Calendar.AUGUST, 1, 12), now, utc))
        assertEquals("9 сентября 2025", DateSeparatorRules.label(zonedMs(utc, 2025, Calendar.SEPTEMBER, 9, 12), now, utc))
        assertEquals(365, DateSeparatorRules.daysAgo(zonedMs(utc, 2025, Calendar.SEPTEMBER, 9, 12), now, utc))
    }

    @Test
    fun sevenDaysAgoIsADateNotAWeekday() {
        assertEquals("2 сентября", DateSeparatorRules.label(zonedMs(utc, 2026, Calendar.SEPTEMBER, 2, 12), now, utc))
        assertEquals(7, DateSeparatorRules.daysAgo(zonedMs(utc, 2026, Calendar.SEPTEMBER, 2, 12), now, utc))
    }

    @Test
    fun berlinSpringForwardCountsCalendarDays() {
        // 2026-03-29 is EU spring-forward (CET 02:00 → CEST 03:00). A 23-hour
        // local day must not collapse yesterday into Сегодня via 86400000 truncation.
        val nowBerlin = zonedMs(berlin, 2026, Calendar.MARCH, 29, 15)
        val today = zonedMs(berlin, 2026, Calendar.MARCH, 29, 1)
        val yesterday = zonedMs(berlin, 2026, Calendar.MARCH, 28, 23)
        val twoAgo = zonedMs(berlin, 2026, Calendar.MARCH, 27, 12)
        val threeAgo = zonedMs(berlin, 2026, Calendar.MARCH, 26, 12)
        val fourAgo = zonedMs(berlin, 2026, Calendar.MARCH, 25, 12)
        val fiveAgo = zonedMs(berlin, 2026, Calendar.MARCH, 24, 12)
        val sixAgo = zonedMs(berlin, 2026, Calendar.MARCH, 23, 12)
        val sevenAgo = zonedMs(berlin, 2026, Calendar.MARCH, 22, 12)

        assertEquals(0, DateSeparatorRules.daysAgo(today, nowBerlin, berlin))
        assertEquals("Сегодня", DateSeparatorRules.label(today, nowBerlin, berlin))
        assertEquals("2026-03-29", DateSeparatorRules.dayKey(today, berlin))

        assertEquals(1, DateSeparatorRules.daysAgo(yesterday, nowBerlin, berlin))
        assertEquals("Вчера", DateSeparatorRules.label(yesterday, nowBerlin, berlin))
        assertEquals("2026-03-28", DateSeparatorRules.dayKey(yesterday, berlin))

        assertEquals(2, DateSeparatorRules.daysAgo(twoAgo, nowBerlin, berlin))
        assertEquals("пятница", DateSeparatorRules.label(twoAgo, nowBerlin, berlin))
        assertEquals(3, DateSeparatorRules.daysAgo(threeAgo, nowBerlin, berlin))
        assertEquals("четверг", DateSeparatorRules.label(threeAgo, nowBerlin, berlin))
        assertEquals(4, DateSeparatorRules.daysAgo(fourAgo, nowBerlin, berlin))
        assertEquals("среда", DateSeparatorRules.label(fourAgo, nowBerlin, berlin))
        assertEquals(5, DateSeparatorRules.daysAgo(fiveAgo, nowBerlin, berlin))
        assertEquals("вторник", DateSeparatorRules.label(fiveAgo, nowBerlin, berlin))
        assertEquals(6, DateSeparatorRules.daysAgo(sixAgo, nowBerlin, berlin))
        assertEquals("понедельник", DateSeparatorRules.label(sixAgo, nowBerlin, berlin))

        assertEquals(7, DateSeparatorRules.daysAgo(sevenAgo, nowBerlin, berlin))
        assertEquals("22 марта", DateSeparatorRules.label(sevenAgo, nowBerlin, berlin))
        assertEquals("2026-03-22", DateSeparatorRules.dayKey(sevenAgo, berlin))

        val items = DateSeparatorRules.items(listOf(msg("y", yesterday), msg("t", today)), nowBerlin, berlin)
        val yChip = items[0] as ChatThreadItem.Day
        val tChip = items[2] as ChatThreadItem.Day
        assertEquals("Вчера", yChip.label)
        assertEquals("2026-03-28", yChip.dayKey)
        assertEquals("Сегодня", tChip.label)
        assertEquals("2026-03-29", tChip.dayKey)
    }

    @Test
    fun moscowWithoutDstStillMatchesCalendarDays() {
        val nowMsk = zonedMs(moscow, 2026, Calendar.MARCH, 29, 15)
        assertEquals("Сегодня", DateSeparatorRules.label(zonedMs(moscow, 2026, Calendar.MARCH, 29, 1), nowMsk, moscow))
        assertEquals(1, DateSeparatorRules.daysAgo(zonedMs(moscow, 2026, Calendar.MARCH, 28, 23), nowMsk, moscow))
        assertEquals("Вчера", DateSeparatorRules.label(zonedMs(moscow, 2026, Calendar.MARCH, 28, 23), nowMsk, moscow))
        assertEquals(7, DateSeparatorRules.daysAgo(zonedMs(moscow, 2026, Calendar.MARCH, 22, 12), nowMsk, moscow))
        assertEquals("22 марта", DateSeparatorRules.label(zonedMs(moscow, 2026, Calendar.MARCH, 22, 12), nowMsk, moscow))
    }

    @Test
    fun shouldShowOnDayChangeOnly() {
        val first = msg("a", zonedMs(utc, 2026, Calendar.SEPTEMBER, 8, 10))
        val same = msg("b", zonedMs(utc, 2026, Calendar.SEPTEMBER, 8, 22))
        val next = msg("c", zonedMs(utc, 2026, Calendar.SEPTEMBER, 9, 0))
        assertTrue(DateSeparatorRules.shouldShow(null, first, utc))
        assertFalse(DateSeparatorRules.shouldShow(first, same, utc))
        assertTrue(DateSeparatorRules.shouldShow(same, next, utc))
        assertTrue(DateSeparatorRules.sameDay(first.timestampMs, same.timestampMs, utc))
        assertFalse(DateSeparatorRules.sameDay(same.timestampMs, next.timestampMs, utc))
    }

    @Test
    fun itemsInsertChipPerDay() {
        val a = msg("a", zonedMs(utc, 2026, Calendar.SEPTEMBER, 8, 10))
        val b = msg("b", zonedMs(utc, 2026, Calendar.SEPTEMBER, 8, 11))
        val c = msg("c", zonedMs(utc, 2026, Calendar.SEPTEMBER, 9, 9))
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
            val t0 = zonedMs(utc, 2026, Calendar.SEPTEMBER, 8, 23, 58)
            val t1 = zonedMs(utc, 2026, Calendar.SEPTEMBER, 9, 0, 2)
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

    private fun zonedMs(tz: TimeZone, year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance(tz).apply {
            clear()
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
