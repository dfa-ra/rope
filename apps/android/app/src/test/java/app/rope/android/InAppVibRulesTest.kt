package app.rope.android

import app.rope.android.data.InAppVibRules
import app.rope.android.data.LocalStore
import app.rope.android.data.NotifyRules
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InAppVibRulesTest {
    @Test
    fun kvDefaultsOnAndOffIsZero() {
        assertTrue(InAppVibRules.enabledFromKv(null))
        assertTrue(InAppVibRules.enabledFromKv("1"))
        assertFalse(InAppVibRules.enabledFromKv("0"))
    }

    @Test
    fun vibrateOnlyWhenHeadsUpWouldAlert() {
        val alert = NotifyRules.shouldAlert(chatOpen = false, appForeground = false, muted = false)
        assertTrue(alert)
        assertTrue(InAppVibRules.shouldVibrate(alert, vibrateEnabled = true))
        assertFalse(InAppVibRules.shouldVibrate(alert, vibrateEnabled = false))
        assertFalse(InAppVibRules.shouldVibrate(alert = false, vibrateEnabled = true))
        val muted = NotifyRules.shouldAlert(chatOpen = false, appForeground = false, muted = true)
        assertFalse(InAppVibRules.shouldVibrate(muted, vibrateEnabled = true))
        val global = NotifyRules.shouldAlert(
            chatOpen = false,
            appForeground = false,
            muted = false,
            globalMuted = true,
        )
        assertFalse(InAppVibRules.shouldVibrate(global, vibrateEnabled = true))
        val open = NotifyRules.shouldAlert(chatOpen = true, appForeground = true, muted = false)
        assertFalse(InAppVibRules.shouldVibrate(open, vibrateEnabled = true))
    }

    @Test
    fun patternIsShortPulseOrSilent() {
        assertArrayEquals(longArrayOf(0, 40, 80, 40), InAppVibRules.pattern(true))
        assertArrayEquals(longArrayOf(0), InAppVibRules.pattern(false))
        assertTrue(InAppVibRules.hint().contains("баннер"))
        assertFalse(InAppVibRules.hint().contains("FCM"))
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
    }
}
