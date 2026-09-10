package app.rope.android.data

/**
 * Invite pane TTL chips. POST /v1/invites already takes ttl_seconds.
 * Capped at 24h to match the relay. LocalStore kv, no bump.
 */
object InviteTtlRules {
    const val KEY = "invite_ttl_seconds"
    const val TITLE = "Срок ссылки"
    const val DEFAULT = 3600
    const val MAX = 24 * 3600

    data class Chip(val seconds: Int, val label: String)

    val CHIPS = listOf(
        Chip(3600, "1 час"),
        Chip(8 * 3600, "8 часов"),
        Chip(MAX, "1 сутки"),
    )

    fun parse(raw: String?): Int {
        if (raw.isNullOrBlank()) return DEFAULT
        if ('\n' in raw || '\r' in raw || '\u0000' in raw) return DEFAULT
        val v = raw.trim().toIntOrNull() ?: return DEFAULT
        return clamp(v)
    }

    fun clamp(seconds: Int): Int =
        CHIPS.find { it.seconds == seconds }?.seconds ?: DEFAULT

    fun label(seconds: Int): String =
        CHIPS.find { it.seconds == clamp(seconds) }?.label ?: CHIPS.first().label

    fun enabled(busy: Boolean): Boolean = !busy
}
