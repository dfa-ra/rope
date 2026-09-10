package app.rope.android

import app.rope.android.data.InAppSoundRules
import app.rope.android.data.LocalStore
import app.rope.android.data.NotifyRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InAppSoundRulesTest {
    @Test
    fun kvDefaultsOnAndOffIsZero() {
        assertTrue(InAppSoundRules.enabledFromKv(null))
        assertTrue(InAppSoundRules.enabledFromKv("1"))
        assertFalse(InAppSoundRules.enabledFromKv("0"))
    }

    @Test
    fun playOnlyWhenForegroundHeadsUpWouldAlert() {
        val alert = NotifyRules.shouldAlert(chatOpen = false, appForeground = true, muted = false)
        assertTrue(alert)
        assertTrue(InAppSoundRules.shouldPlay(alert, soundEnabled = true, appForeground = true))
        assertFalse(InAppSoundRules.shouldPlay(alert, soundEnabled = false, appForeground = true))
        assertFalse(InAppSoundRules.shouldPlay(alert = false, soundEnabled = true, appForeground = true))
        val muted = NotifyRules.shouldAlert(chatOpen = false, appForeground = true, muted = true)
        assertFalse(InAppSoundRules.shouldPlay(muted, soundEnabled = true, appForeground = true))
        val global = NotifyRules.shouldAlert(
            chatOpen = false,
            appForeground = true,
            muted = false,
            globalMuted = true,
        )
        assertFalse(InAppSoundRules.shouldPlay(global, soundEnabled = true, appForeground = true))
        val open = NotifyRules.shouldAlert(chatOpen = true, appForeground = true, muted = false)
        assertFalse(InAppSoundRules.shouldPlay(open, soundEnabled = true, appForeground = true))
        val background = NotifyRules.shouldAlert(chatOpen = false, appForeground = false, muted = false)
        assertTrue(background)
        assertFalse(InAppSoundRules.shouldPlay(background, soundEnabled = true, appForeground = false))
    }

    @Test
    fun foregroundSuppressesChannelSound() {
        assertTrue(InAppSoundRules.suppressChannelSound(true))
        assertFalse(InAppSoundRules.suppressChannelSound(false))
        assertEquals(27, InAppSoundRules.TONE)
        assertEquals(120, InAppSoundRules.DURATION_MS)
        assertTrue(InAppSoundRules.hint().contains("баннер"))
        assertFalse(InAppSoundRules.hint().contains("FCM"))
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
    }
}
