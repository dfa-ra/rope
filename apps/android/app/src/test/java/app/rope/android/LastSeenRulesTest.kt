package app.rope.android

import app.rope.android.data.DateSeparatorRules
import app.rope.android.data.MessageTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class LastSeenRulesTest {
    @Test
    fun onlineAndMissingStamp() {
        assertEquals(MessageTime.ONLINE, MessageTime.lastSeenLabel("", true))
        assertEquals(MessageTime.OFFLINE_HINT, MessageTime.lastSeenLabel("", false))
        assertEquals(MessageTime.OFFLINE_HINT, MessageTime.lastSeenLabel("not-a-date", false))
    }

    @Test
    fun justNowWithinAMinute() {
        val seen = cal(14, 5)
        val rfc = rfc(seen)
        assertEquals("был(а) только что", MessageTime.lastSeenLabel(rfc, false, seen.timeInMillis))
        assertEquals(
            "был(а) только что",
            MessageTime.lastSeenLabel(rfc, false, seen.timeInMillis + 59_000L),
        )
        assertEquals(
            "был(а) в 14:05",
            MessageTime.lastSeenLabel(rfc, false, seen.timeInMillis + 60_000L),
        )
    }

    @Test
    fun yesterdayUsesParticle() {
        val seen = cal(14, 5)
        val now = Calendar.getInstance().apply {
            timeInMillis = seen.timeInMillis
            add(Calendar.DATE, 1)
            set(Calendar.HOUR_OF_DAY, 10)
        }
        val label = MessageTime.lastSeenWhen(seen.timeInMillis, now.timeInMillis)
        assertEquals("был(а) вчера в 14:05", label)
        assertEquals(1, DateSeparatorRules.daysAgo(seen.timeInMillis, now.timeInMillis))
    }

    @Test
    fun weekdayKeepsClock() {
        val seen = cal(14, 5)
        val now = Calendar.getInstance().apply {
            timeInMillis = seen.timeInMillis
            add(Calendar.DATE, 3)
            set(Calendar.HOUR_OF_DAY, 10)
        }
        val label = MessageTime.lastSeenWhen(seen.timeInMillis, now.timeInMillis)
        val weekday = DateSeparatorRules.label(seen.timeInMillis, now.timeInMillis)
        assertEquals("был(а) $weekday в 14:05", label)
        assertTrue(label.contains("в 14:05"))
    }

    @Test
    fun olderDropsClock() {
        val seen = cal(14, 5)
        val now = Calendar.getInstance().apply {
            timeInMillis = seen.timeInMillis
            add(Calendar.DATE, 20)
        }
        val label = MessageTime.lastSeenWhen(seen.timeInMillis, now.timeInMillis)
        val day = DateSeparatorRules.label(seen.timeInMillis, now.timeInMillis)
        assertEquals("был(а) $day", label)
        assertTrue(!label.contains("14:05"))
    }

    private fun cal(hour: Int, minute: Int): Calendar =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    private fun rfc(cal: Calendar): String =
        java.time.Instant.ofEpochMilli(cal.timeInMillis).toString()
}
