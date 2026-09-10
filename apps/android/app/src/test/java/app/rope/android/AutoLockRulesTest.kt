package app.rope.android

import app.rope.android.data.AutoLockRules
import app.rope.android.data.LocalStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoLockRulesTest {
    @Test
    fun idleTimeoutLocksAfterBackground() {
        assertEquals(AutoLockRules.OFF, AutoLockRules.parse(null))
        assertEquals(AutoLockRules.OFF, AutoLockRules.parse("a\n1"))
        assertEquals(60_000L, AutoLockRules.parse("60000"))
        assertFalse(AutoLockRules.enabled(AutoLockRules.OFF))
        assertTrue(AutoLockRules.enabled(0L))
        assertFalse(
            AutoLockRules.shouldLock(
                timeoutMs = 0L,
                lastBackgroundedAt = 0L,
                now = 10L,
                signedIn = true,
                inCall = false,
            ),
        )
        assertTrue(
            AutoLockRules.shouldLock(
                timeoutMs = 0L,
                lastBackgroundedAt = 1L,
                now = 2L,
                signedIn = true,
                inCall = false,
            ),
        )
        assertFalse(
            AutoLockRules.shouldLock(
                timeoutMs = 60_000L,
                lastBackgroundedAt = 100L,
                now = 101L,
                signedIn = true,
                inCall = false,
            ),
        )
        assertTrue(
            AutoLockRules.shouldLock(
                timeoutMs = 60_000L,
                lastBackgroundedAt = 100L,
                now = 100L + 60_000L,
                signedIn = true,
                inCall = false,
            ),
        )
        assertFalse(
            AutoLockRules.shouldLock(
                timeoutMs = 0L,
                lastBackgroundedAt = 1L,
                now = 2L,
                signedIn = true,
                inCall = true,
            ),
        )
        assertFalse(
            AutoLockRules.shouldLock(
                timeoutMs = 0L,
                lastBackgroundedAt = 1L,
                now = 2L,
                signedIn = false,
                inCall = false,
            ),
        )
        assertEquals("Сразу", AutoLockRules.label(0L))
        assertEquals("Автоблокировка", AutoLockRules.TITLE)
        assertTrue(AutoLockRules.hint().contains("Код-пароль"))
        assertEquals(6, LocalStore.VERSION)
    }
}
