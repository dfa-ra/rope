package app.rope.android

import app.rope.android.data.AppLockRules
import app.rope.android.data.AppLockState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class AppLockRulesTest {
    @Test
    fun pinOk4to8() {
        assertTrue(AppLockRules.pinOk("1234"))
        assertTrue(AppLockRules.pinOk("123456"))
        assertTrue(AppLockRules.pinOk("12345678"))
        assertFalse(AppLockRules.pinOk("123"))
        assertFalse(AppLockRules.pinOk("123456789"))
        assertFalse(AppLockRules.pinOk(""))
    }

    @Test
    fun pinRejectsLetters() {
        assertFalse(AppLockRules.pinOk("12ab"))
        assertFalse(AppLockRules.pinOk("abcd"))
        assertFalse(AppLockRules.pinOk("12 34"))
        assertFalse(AppLockRules.pinOk("12.4"))
    }

    @Test
    fun biometricAllowedOnlyIfPinSet() {
        assertFalse(AppLockRules.biometricAllowed(pinSet = false, hardware = true))
        assertFalse(AppLockRules.biometricAllowed(pinSet = true, hardware = false))
        assertTrue(AppLockRules.biometricAllowed(pinSet = true, hardware = true))
    }

    @Test
    fun shouldLockOnStopTimeout0() {
        assertTrue(AppLockRules.shouldLockOnStop(true, 0L, lastBackgroundedAt = 1L, now = 2L))
        assertFalse(AppLockRules.shouldLockOnStop(false, 0L, 1L, 2L))
        assertFalse(AppLockRules.shouldLockOnStop(true, 60_000L, 100L, 101L))
        assertTrue(AppLockRules.shouldLockOnStop(true, 60_000L, 100L, 100L + 60_000L))
    }

    @Test
    fun hmacMismatchNoLoad() {
        val expected = AppLockRules.toHex(byteArrayOf(1, 2, 3, 4))
        val good = byteArrayOf(1, 2, 3, 4)
        val bad = byteArrayOf(1, 2, 3, 5)
        assertTrue(AppLockRules.hmacMatches(expected, good))
        assertFalse(AppLockRules.hmacMatches(expected, bad))
        assertFalse(AppLockRules.mayLoadIdentity(hmacOk = false))
        assertTrue(AppLockRules.mayLoadIdentity(hmacOk = true))
        assertTrue(AppLockRules.gateOnStart(true, vaultExists = true, hmacHex = "ab"))
        assertFalse(AppLockRules.gateOnStart(true, vaultExists = true, hmacHex = ""))
        assertFalse(AppLockRules.gateOnStart(true, vaultExists = false, hmacHex = "ab"))
    }

    @Test
    fun lockNullsIdentity() {
        assertNull(AppLockRules.identityAfterLock())
        assertEquals(0, AppLockRules.messagesWhenLocked(locked = true, liveCount = 9))
        assertEquals(9, AppLockRules.messagesWhenLocked(locked = false, liveCount = 9))
    }

    @Test
    fun exportRequiresUnlock() {
        assertTrue(AppLockRules.exportAllowed(locked = false, identityPresent = true))
        assertFalse(AppLockRules.exportAllowed(locked = true, identityPresent = true))
        assertFalse(AppLockRules.exportAllowed(locked = false, identityPresent = false))
    }

    @Test
    fun pinNotEqualRobkPassphraseCopy() {
        val hint = AppLockRules.hint()
        assertTrue(hint.contains("PIN"))
        assertTrue(hint.contains("экспорт"))
        assertFalse(hint.contains("FCM"))
        assertEquals(12, AppLockRules.ROBK_MIN)
        assertTrue(AppLockRules.pinOk("1234"))
        assertTrue("1234".length < AppLockRules.ROBK_MIN)
        assertEquals("Блокировка", AppLockRules.SECTION)
    }

    @Test
    fun isEqualConstantTime() {
        val a = byteArrayOf(9, 8, 7, 6)
        val b = byteArrayOf(9, 8, 7, 6)
        val c = byteArrayOf(9, 8, 7, 0)
        assertTrue(AppLockRules.digestEqual(a, b))
        assertTrue(MessageDigest.isEqual(a, b))
        assertFalse(AppLockRules.digestEqual(a, c))
        val parsed = AppLockRules.parse(
            AppLockState(enabled = true, timeoutMs = 60_000L, hmacHex = "dead").toJson(),
        )
        assertTrue(parsed.enabled)
        assertEquals(60_000L, parsed.timeoutMs)
        assertEquals("dead", parsed.hmacHex)
        assertFalse(parsed.toJson().contains("pin", ignoreCase = true) && parsed.toJson().contains("\"pin\""))
        assertFalse(parsed.toJson().contains("\"pin\""))
        assertEquals(30_000L, AppLockRules.backoffMs(5))
        assertEquals(120_000L, AppLockRules.backoffMs(8))
        assertEquals(300_000L, AppLockRules.backoffMs(10))
        assertEquals("Rope", AppLockRules.shadeTitle(true, "Анна"))
        assertEquals("Новое сообщение", AppLockRules.shadeBody(true, "секрет"))
    }
}
