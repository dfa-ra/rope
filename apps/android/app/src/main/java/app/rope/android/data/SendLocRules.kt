package app.rope.android.data

import java.util.Locale

data class SendLocFix(
    val lat: Double,
    val lon: Double,
    val timeMs: Long = 0L,
)

/**
 * Attach-sheet **Геопозиция**. Sends an OpenStreetMap HTTPS pin as text
 * (no map SDK, no new MessageKind). LocalStore stays v6.
 */
object SendLocRules {
    const val LABEL = "Геопозиция"
    const val NEED_PERM = "Нужен доступ к геопозиции."
    const val NEED_FIX = "Не удалось определить местоположение."
    val PROVIDERS = listOf("gps", "network", "passive")

    fun permissionOk(fine: Boolean, coarse: Boolean): Boolean = fine || coarse

    fun valid(lat: Double, lon: Double): Boolean =
        lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0

    fun formatCoord(value: Double): String = String.format(Locale.US, "%.6f", value)

    fun text(lat: Double, lon: Double): String? {
        if (!valid(lat, lon)) return null
        val la = formatCoord(lat)
        val lo = formatCoord(lon)
        val url = "https://www.openstreetmap.org/?mlat=$la&mlon=$lo#map=16/$la/$lo"
        if (url.any { it == '\n' || it == '\r' || it == '\u0000' }) return null
        return url
    }

    fun pick(fixes: List<SendLocFix>): SendLocFix? =
        fixes.filter { valid(it.lat, it.lon) }.maxByOrNull { it.timeMs }

    fun noticeDenied(): String = NEED_PERM

    fun noticeMissing(): String = NEED_FIX
}
