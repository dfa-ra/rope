package app.rope.android.data

import org.json.JSONObject
import java.security.MessageDigest

/**
 * Telegram-like in-app passcode. PIN 4–8 digits, not the phone PIN, not ROBK.
 * Kv `app_lock` stores HMAC only — never the PIN. LocalStore stays v6.
 */
data class AppLockState(
    val enabled: Boolean = false,
    val biometric: Boolean = false,
    val timeoutMs: Long = 0L,
    val failCount: Int = 0,
    val lockedUntilMs: Long = 0L,
    val hmacHex: String = "",
    val pinLen: Int = 0,
) {
    fun toJson(): String = JSONObject()
        .put("enabled", enabled)
        .put("biometric", biometric)
        .put("timeout", timeoutMs)
        .put("fail_count", failCount)
        .put("locked_until", lockedUntilMs)
        .put("hmac", hmacHex)
        .put("pin_len", pinLen)
        .toString()

    fun toUi(): AppLockUi = AppLockUi(
        enabled = enabled,
        biometric = biometric,
        timeoutMs = timeoutMs,
        failCount = failCount,
        lockedUntilMs = lockedUntilMs,
        pinSet = hmacHex.isNotBlank(),
        pinLen = pinLen,
    )
}

data class AppLockUi(
    val enabled: Boolean = false,
    val biometric: Boolean = false,
    val timeoutMs: Long = 0L,
    val failCount: Int = 0,
    val lockedUntilMs: Long = 0L,
    val pinSet: Boolean = false,
    val pinLen: Int = 0,
)

object AppLockRules {
    const val SECTION = "Блокировка"
    const val PIN_TITLE = "Код-пароль"
    const val BIO_TITLE = "Отпечаток"
    const val TIMEOUT_TITLE = "Автоблокировка"
    const val WRONG = "Неверный код"
    const val WAIT = "Подождите"
    const val ROBK_MIN = 12
    const val PIN_MIN = 4
    const val PIN_MAX = 8
    const val SHADE_TITLE = "Rope"
    const val SHADE_BODY = "Новое сообщение"

    fun hint(): String =
        "PIN остаётся на телефоне. Это не шифрование устройства и не пароль экспорта."

    val timeouts: List<Pair<Long, String>> = listOf(
        0L to "Сразу",
        60_000L to "1 мин",
        300_000L to "5 мин",
        900_000L to "15 мин",
    )

    fun pinOk(pin: String): Boolean =
        pin.length in PIN_MIN..PIN_MAX && pin.all { it in '0'..'9' }

    fun timeoutMs(choice: Long): Long =
        timeouts.firstOrNull { it.first == choice }?.first ?: 0L

    fun backoffMs(failCount: Int): Long = when {
        failCount < 5 -> 0L
        failCount < 8 -> 30_000L
        failCount < 10 -> 120_000L
        else -> 300_000L
    }

    fun shouldLockOnStop(
        enabled: Boolean,
        timeoutMs: Long,
        lastBackgroundedAt: Long,
        now: Long,
    ): Boolean {
        if (!enabled) return false
        if (timeoutMs <= 0L) return true
        if (lastBackgroundedAt <= 0L) return false
        return now - lastBackgroundedAt >= timeoutMs
    }

    fun biometricAllowed(pinSet: Boolean, hardware: Boolean): Boolean = pinSet && hardware

    fun digestEqual(a: ByteArray, b: ByteArray): Boolean = MessageDigest.isEqual(a, b)

    fun toHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    fun fromHex(hex: String): ByteArray? {
        val clean = hex.trim().lowercase()
        if (clean.isEmpty() || clean.length % 2 != 0) return null
        if (clean.any { it !in '0'..'9' && it !in 'a'..'f' }) return null
        return ByteArray(clean.length / 2) { i ->
            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    fun hmacMatches(expectedHex: String, computed: ByteArray): Boolean {
        val expected = fromHex(expectedHex) ?: return false
        return digestEqual(expected, computed)
    }

    fun mayLoadIdentity(hmacOk: Boolean): Boolean = hmacOk

    fun gateOnStart(enabled: Boolean, vaultExists: Boolean, hmacHex: String): Boolean =
        enabled && vaultExists && hmacHex.isNotBlank()

    fun exportAllowed(locked: Boolean, identityPresent: Boolean): Boolean =
        !locked && identityPresent

    fun identityAfterLock(): Any? = null

    fun messagesWhenLocked(locked: Boolean, liveCount: Int): Int = if (locked) 0 else liveCount

    fun shadeTitle(locked: Boolean, title: String): String = if (locked) SHADE_TITLE else title

    fun shadeBody(locked: Boolean, body: String): String = if (locked) SHADE_BODY else body

    fun inputBlocked(now: Long, lockedUntilMs: Long): Boolean = now < lockedUntilMs

    fun afterFail(cur: AppLockState, now: Long): AppLockState {
        val fails = cur.failCount + 1
        return cur.copy(failCount = fails, lockedUntilMs = now + backoffMs(fails))
    }

    fun afterUnlock(cur: AppLockState): AppLockState =
        cur.copy(failCount = 0, lockedUntilMs = 0L)

    fun parse(raw: String?): AppLockState {
        if (raw.isNullOrBlank()) return AppLockState()
        return try {
            val o = JSONObject(raw)
            AppLockState(
                enabled = o.optBoolean("enabled"),
                biometric = o.optBoolean("biometric"),
                timeoutMs = o.optLong("timeout"),
                failCount = o.optInt("fail_count"),
                lockedUntilMs = o.optLong("locked_until"),
                hmacHex = o.optString("hmac"),
                pinLen = o.optInt("pin_len"),
            )
        } catch (_: Exception) {
            AppLockState()
        }
    }
}
