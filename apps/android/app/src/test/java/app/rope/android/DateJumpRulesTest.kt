package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.DateJumpRules
import app.rope.android.data.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DateJumpRulesTest {
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")
    private val now = zonedMs(2026, Calendar.SEPTEMBER, 9, 15)

    @Test
    fun newestDayFirstAndJumpsToFirstMessageOfDay() {
        val todayA = msg("t1", zonedMs(2026, Calendar.SEPTEMBER, 9, 8))
        val todayB = msg("t2", zonedMs(2026, Calendar.SEPTEMBER, 9, 12))
        val yesterday = msg("y1", zonedMs(2026, Calendar.SEPTEMBER, 8, 18))
        val days = DateJumpRules.choices(listOf(yesterday, todayA, todayB), now, utc)
        assertEquals(listOf("2026-09-09", "2026-09-08"), days.map { it.dayKey })
        assertEquals("Сегодня", days[0].label)
        assertEquals("Вчера", days[1].label)
        assertEquals("t1", days[0].messageId)
        assertEquals(
            "t1",
            DateJumpRules.firstId(listOf(yesterday, todayA, todayB), "2026-09-09", now, utc),
        )
        assertEquals("y1", DateJumpRules.firstId(listOf(yesterday, todayA, todayB), "2026-09-08", now, utc))
        assertNull(DateJumpRules.firstId(listOf(yesterday, todayA, todayB), "2026-09-01", now, utc))
    }

    @Test
    fun skipsDeletedAndHidesEmpty() {
        val gone = msg("gone", zonedMs(2026, Calendar.SEPTEMBER, 9, 8)).copy(deleted = true)
        val live = msg("live", zonedMs(2026, Calendar.SEPTEMBER, 8, 8))
        assertFalse(DateJumpRules.showButton(emptyList()))
        assertFalse(DateJumpRules.showButton(listOf(gone)))
        assertTrue(DateJumpRules.showButton(listOf(gone, live)))
        val days = DateJumpRules.choices(listOf(gone, live), now, utc)
        assertEquals(listOf("2026-09-08"), days.map { it.dayKey })
        assertEquals("live", days.single().messageId)
    }

    private fun msg(id: String, ts: Long) = ChatMessage(
        id = id,
        peerDeviceId = "p",
        outgoing = false,
        text = id,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = ts,
    )

    private fun zonedMs(year: Int, month: Int, day: Int, hour: Int): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month, day, hour, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
