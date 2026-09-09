package app.rope.android.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** One row in a chat thread: a day chip or a message bubble. */
sealed class ChatThreadItem {
    abstract val key: String

    data class Day(val dayKey: String, val label: String) : ChatThreadItem() {
        override val key: String get() = "day-$dayKey"
    }

    data class Bubble(val msg: ChatMessage) : ChatThreadItem() {
        override val key: String get() = msg.id
    }
}

data class ChatDayGroup(
    val dayKey: String,
    val label: String,
    val messages: List<ChatMessage>,
)

/**
 * Telegram-like day chips in a thread: Сегодня, Вчера, weekday if within 6 days,
 * otherwise `d MMMM` (same year) or `d MMMM yyyy`.
 */
object DateSeparatorRules {
    @Suppress("DEPRECATION")
    val RU: Locale = Locale("ru", "RU")

    fun dayKey(timestampMs: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = timestampMs }
        return "%04d-%02d-%02d".format(
            Locale.US,
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
        )
    }

    fun sameDay(
        aMs: Long,
        bMs: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): Boolean = dayKey(aMs, timeZone) == dayKey(bMs, timeZone)

    fun shouldShow(
        prev: ChatMessage?,
        current: ChatMessage,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): Boolean = prev == null || !sameDay(prev.timestampMs, current.timestampMs, timeZone)

    /** Count of local calendar-day boundaries between [timestampMs] and [nowMs]. */
    fun daysAgo(
        timestampMs: Long,
        nowMs: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): Int {
        val then = Calendar.getInstance(timeZone).apply { timeInMillis = timestampMs }
        val now = Calendar.getInstance(timeZone).apply { timeInMillis = nowMs }
        val thenYear = then.get(Calendar.YEAR)
        val nowYear = now.get(Calendar.YEAR)
        var days = now.get(Calendar.DAY_OF_YEAR) - then.get(Calendar.DAY_OF_YEAR)
        val probe = Calendar.getInstance(timeZone)
        var year = thenYear
        while (year < nowYear) {
            probe.clear()
            probe.set(year, Calendar.JANUARY, 1)
            days += probe.getActualMaximum(Calendar.DAY_OF_YEAR)
            year++
        }
        year = nowYear
        while (year < thenYear) {
            probe.clear()
            probe.set(year, Calendar.JANUARY, 1)
            days -= probe.getActualMaximum(Calendar.DAY_OF_YEAR)
            year++
        }
        return days
    }

    fun label(
        timestampMs: Long,
        nowMs: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = RU,
    ): String {
        val ago = daysAgo(timestampMs, nowMs, timeZone)
        return when {
            ago == 0 -> "Сегодня"
            ago == 1 -> "Вчера"
            ago in 2..6 -> format(timestampMs, "EEEE", timeZone, locale)
            sameYear(timestampMs, nowMs, timeZone) -> format(timestampMs, "d MMMM", timeZone, locale)
            else -> format(timestampMs, "d MMMM yyyy", timeZone, locale)
        }
    }

    fun grouped(
        messages: List<ChatMessage>,
        nowMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = RU,
    ): List<ChatDayGroup> {
        if (messages.isEmpty()) return emptyList()
        val out = mutableListOf<ChatDayGroup>()
        var key: String? = null
        var text = ""
        var bucket = mutableListOf<ChatMessage>()
        for (m in messages) {
            val next = dayKey(m.timestampMs, timeZone)
            if (next != key) {
                if (bucket.isNotEmpty() && key != null) {
                    out += ChatDayGroup(key, text, bucket.toList())
                }
                key = next
                text = label(m.timestampMs, nowMs, timeZone, locale)
                bucket = mutableListOf()
            }
            bucket += m
        }
        if (bucket.isNotEmpty() && key != null) {
            out += ChatDayGroup(key, text, bucket.toList())
        }
        return out
    }

    fun items(
        messages: List<ChatMessage>,
        nowMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = RU,
    ): List<ChatThreadItem> = flatten(grouped(messages, nowMs, timeZone, locale))

    fun flatten(groups: List<ChatDayGroup>): List<ChatThreadItem> {
        val out = ArrayList<ChatThreadItem>(groups.sumOf { 1 + it.messages.size })
        for (g in groups) {
            out += ChatThreadItem.Day(g.dayKey, g.label)
            for (m in g.messages) out += ChatThreadItem.Bubble(m)
        }
        return out
    }

    fun indexOfMessage(items: List<ChatThreadItem>, id: String): Int =
        items.indexOfFirst { it is ChatThreadItem.Bubble && it.msg.id == id }

    private fun sameYear(aMs: Long, bMs: Long, timeZone: TimeZone): Boolean {
        val a = Calendar.getInstance(timeZone).apply { timeInMillis = aMs }
        val b = Calendar.getInstance(timeZone).apply { timeInMillis = bMs }
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
    }

    private fun format(ms: Long, pattern: String, timeZone: TimeZone, locale: Locale): String {
        val fmt = SimpleDateFormat(pattern, locale)
        fmt.timeZone = timeZone
        return fmt.format(Date(ms))
    }
}
