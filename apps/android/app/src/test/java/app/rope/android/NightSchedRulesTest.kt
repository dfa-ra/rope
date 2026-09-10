package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.NightSchedRules
import app.rope.android.data.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NightSchedRulesTest {
    @Test
    fun localStoreStaysV6() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, BuildConfig.VERSION_CODE)
    }

    @Test
    fun darkFrom22To7() {
        assertTrue(NightSchedRules.isNight(22))
        assertTrue(NightSchedRules.isNight(23))
        assertTrue(NightSchedRules.isNight(0))
        assertTrue(NightSchedRules.isNight(6))
        assertFalse(NightSchedRules.isNight(7))
        assertFalse(NightSchedRules.isNight(21))
        assertEquals(ThemeMode.DARK, NightSchedRules.scheduledTheme(22))
        assertEquals(ThemeMode.LIGHT, NightSchedRules.scheduledTheme(12))
        assertEquals(ThemeMode.DARK, NightSchedRules.displayed(true, 23, ThemeMode.LIGHT))
        assertEquals(ThemeMode.LIGHT, NightSchedRules.displayed(false, 23, ThemeMode.LIGHT))
        assertEquals("Ночь с 22:00", NightSchedRules.TITLE)
    }
}
