package app.rope.android.data

/**
 * Telegram-like sticky date chip while scrolling a thread. Shown when the
 * inline day separator has scrolled off the top — no LocalStore bump.
 */
object ScrollDateRules {
    const val CHIP = "Дата"

    fun stickyLabel(items: List<ChatThreadItem>, firstVisibleIndex: Int): String? {
        if (items.isEmpty() || firstVisibleIndex < 0) return null
        val i = firstVisibleIndex.coerceAtMost(items.lastIndex)
        if (items[i] is ChatThreadItem.Day) return null
        for (j in i downTo 0) {
            val row = items[j]
            if (row is ChatThreadItem.Day) return row.label
        }
        return null
    }
}
