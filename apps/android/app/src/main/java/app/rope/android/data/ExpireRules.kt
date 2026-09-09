package app.rope.android.data

import org.json.JSONObject
import java.io.File

data class ExpireStamp(
    val ttlSec: Int = 0,
    val expMs: Long? = null,
    val mid: String? = null,
) {
    val active: Boolean get() = ttlSec > 0 || (expMs != null && expMs > 0L)
}

/**
 * Telegram-like auto-delete: countdown starts at send (`exp = timestamp_ms + ttl`).
 * Inner JSON only — never an envelope header. Go never sees these keys.
 */
object ExpireRules {
    const val OFF = 0
    const val HOURS_24 = 86_400
    const val DAYS_7 = 604_800
    const val DAYS_31 = 2_678_400
    const val SWEEP_MS = 60_000L
    const val ROW_TITLE = "Автоудаление"
    const val CONFIRM =
        "Новые сообщения удалятся у всех в этом чате. Старые останутся. Сервер текст не видит."
    const val DELETED_STUB = "Сообщение удалено"

    data class DurationOption(val ttlSec: Int, val label: String)

    val OPTIONS = listOf(
        DurationOption(OFF, "Выкл"),
        DurationOption(HOURS_24, "24 часа"),
        DurationOption(DAYS_7, "7 дней"),
        DurationOption(DAYS_31, "1 месяц"),
    )

    fun normalizeTtl(ttlSec: Int): Int =
        OPTIONS.find { it.ttlSec == ttlSec }?.ttlSec ?: OFF

    fun expiresAtMs(timestampMs: Long, ttlSec: Int): Long? {
        if (ttlSec <= 0) return null
        return timestampMs + ttlSec.toLong() * 1000L
    }

    fun due(expMs: Long?, nowMs: Long): Boolean =
        expMs != null && expMs > 0L && nowMs >= expMs

    fun resolveExp(expMs: Long?, ttlSec: Int, timestampMs: Long): Long? {
        if (expMs != null && expMs > 0L) return expMs
        return expiresAtMs(timestampMs, ttlSec)
    }

    fun stamp(ttlSec: Int, timestampMs: Long, mid: String? = null): ExpireStamp {
        val ttl = if (ttlSec > 0) ttlSec else 0
        return ExpireStamp(ttl, expiresAtMs(timestampMs, ttl), JsonIds.optional(mid))
    }

    /** Tombstone wipes even if local `exp` is still in the future. */
    fun shouldWipe(expMs: Long?, nowMs: Long, tombstone: Boolean): Boolean {
        if (tombstone) return true
        return due(expMs, nowMs)
    }

    fun shouldSendControl(chatId: String?): Boolean = !SavedMessagesRules.skipNetwork(chatId)

    fun canSetTimer(isGroup: Boolean, canManageGroup: Boolean, saved: Boolean): Boolean {
        if (saved) return true
        if (isGroup) return canManageGroup
        return true
    }

    fun showSettingsRow(isGroup: Boolean, canSet: Boolean, ttlSec: Int): Boolean {
        if (canSet) return true
        return isGroup && ttlSec > 0
    }

    fun senderMaySetTtl(
        isGroup: Boolean,
        senderId: String,
        organizerId: String,
        senderServerRole: String?,
    ): Boolean {
        if (!isGroup) return true
        if (RoleRules.isOwner(senderServerRole)) return true
        return senderId.isNotBlank() && senderId == organizerId
    }

    fun ttlTarget(chatId: String): String {
        if (ChatIds.isGroup(chatId)) return "group:" + ChatIds.rawGroupId(chatId)
        return chatId
    }

    fun chatIdFromTtlTarget(target: String, fallbackPeer: String): String {
        val t = target.trim()
        return when {
            t.startsWith("group:") -> ChatIds.group(t.removePrefix("group:"))
            ChatIds.isGroup(t) || SavedMessagesRules.isSaved(t) -> t
            t.isNotBlank() -> t
            else -> fallbackPeer
        }
    }

    fun rowSubtitle(ttlSec: Int): String = when (normalizeTtl(ttlSec)) {
        OFF -> "Выкл"
        HOURS_24 -> "24 часа"
        DAYS_7 -> "7 дней"
        DAYS_31 -> "1 месяц"
        else -> "Выкл"
    }

    fun guestSubtitle(ttlSec: Int): String = when (normalizeTtl(ttlSec)) {
        HOURS_24 -> "Сообщения исчезают через 24 часа"
        DAYS_7 -> "Сообщения исчезают через 7 дней"
        DAYS_31 -> "Сообщения исчезают через 1 месяц"
        else -> ""
    }

    fun remainingCopy(expMs: Long, nowMs: Long): String {
        val left = expMs - nowMs
        if (left <= 0L) return "истекло"
        val sec = left / 1000L
        val min = sec / 60L
        val hour = min / 60L
        val day = hour / 24L
        return when {
            day >= 1L -> "исчезнет через $day д"
            hour >= 1L -> "исчезнет через $hour ч"
            min >= 1L -> "исчезнет через $min мин"
            else -> "исчезнет через несколько секунд"
        }
    }

    fun put(o: JSONObject, stamp: ExpireStamp) {
        if (stamp.ttlSec > 0) o.put("ttl", stamp.ttlSec)
        stamp.expMs?.takeIf { it > 0L }?.let { o.put("exp", it) }
        JsonIds.optional(stamp.mid)?.let { o.put("mid", it) }
    }

    fun read(o: JSONObject): ExpireStamp {
        val ttl = o.optInt("ttl", 0)
        val exp = if (o.has("exp")) o.optLong("exp") else null
        return ExpireStamp(ttl, exp?.takeIf { it > 0L }, JsonIds.optional(o.optString("mid")))
    }

    fun unlinkIfUnderMedia(localPath: String?, mediaRoot: File): Boolean {
        if (localPath.isNullOrBlank()) return false
        val root = try {
            mediaRoot.canonicalFile
        } catch (_: Exception) {
            return false
        }
        val target = try {
            File(localPath).canonicalFile
        } catch (_: Exception) {
            return false
        }
        val rootPath = root.absolutePath
        val path = target.absolutePath
        val under = path == rootPath || path.startsWith(rootPath + File.separator)
        if (!under) return false
        if (!target.exists()) return true
        return target.delete() || !target.exists()
    }
}
