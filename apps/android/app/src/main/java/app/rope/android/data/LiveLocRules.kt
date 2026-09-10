package app.rope.android.data

import java.util.Locale

data class LiveLocFix(
    val lat: Double,
    val lon: Double,
    val timeMs: Long = 0L,
)

data class LiveLocChoice(
    val label: String,
    val ms: Long,
)

data class LiveLocSession(
    val chatId: String,
    val untilMs: Long,
    val choiceMs: Long,
    val messageId: String? = null,
)

/**
 * Attach-sheet **Трансляция**. Live OSM pin that refreshes until the chosen
 * window ends. Distinct from one-shot Геопозиция. No map SDK, no new
 * MessageKind, no LocalStore bump.
 */
object LiveLocRules {
    const val LABEL = "Трансляция"
    const val STOP = "Остановить"
    const val NEED_PERM = "Нужен доступ к геопозиции."
    const val NEED_FIX = "Не удалось определить местоположение."
    const val PERIOD_MS = 20_000L
    const val MAX_CHAT = 128
    val PROVIDERS = listOf("gps", "network", "passive")
    val CHOICES = listOf(
        LiveLocChoice("15 минут", 15L * 60_000L),
        LiveLocChoice("1 час", 60L * 60_000L),
        LiveLocChoice("8 часов", 8L * 60L * 60_000L),
    )

    fun permissionOk(fine: Boolean, coarse: Boolean): Boolean = fine || coarse

    fun valid(lat: Double, lon: Double): Boolean =
        lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0

    fun formatCoord(value: Double): String = String.format(Locale.US, "%.6f", value)

    fun pin(lat: Double, lon: Double): String? {
        if (!valid(lat, lon)) return null
        val la = formatCoord(lat)
        val lo = formatCoord(lon)
        val url = "https://www.openstreetmap.org/?mlat=$la&mlon=$lo#map=16/$la/$lo"
        if (url.any { it == '\n' || it == '\r' || it == '\u0000' }) return null
        return url
    }

    fun choice(ms: Long): LiveLocChoice? = CHOICES.find { it.ms == ms }

    fun headline(choice: LiveLocChoice): String = "$LABEL · ${choice.label}"

    fun text(lat: Double, lon: Double, choice: LiveLocChoice): String? {
        val url = pin(lat, lon) ?: return null
        val body = "${headline(choice)}\n$url"
        if (body.any { it == '\r' || it == '\u0000' }) return null
        return body
    }

    fun isLive(text: String?): Boolean {
        val t = text?.trim().orEmpty()
        return t.startsWith("$LABEL ·") || t.startsWith("$LABEL\n")
    }

    fun pick(fixes: List<LiveLocFix>): LiveLocFix? =
        fixes.filter { valid(it.lat, it.lon) }.maxByOrNull { it.timeMs }

    fun cleanChatId(raw: String?): String? {
        val t = raw?.trim().orEmpty()
        if (t.isEmpty() || t.length > MAX_CHAT) return null
        if (t.any { it == '\n' || it == '\r' || it == '\u0000' }) return null
        return t
    }

    fun active(untilMs: Long, nowMs: Long): Boolean = nowMs < untilMs

    fun here(session: LiveLocSession?, chatId: String?, nowMs: Long): Boolean {
        val id = cleanChatId(chatId) ?: return false
        val s = session ?: return false
        return s.chatId == id && active(s.untilMs, nowMs)
    }

    fun noticeDenied(): String = NEED_PERM

    fun noticeMissing(): String = NEED_FIX
}
