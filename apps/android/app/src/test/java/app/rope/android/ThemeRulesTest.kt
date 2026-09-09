package app.rope.android

import app.rope.android.data.ThemeMode
import app.rope.android.data.ThemeRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeRulesTest {
    @Test
    fun parseStoredMapsAliases() {
        assertEquals(ThemeMode.LIGHT, ThemeRules.parseStored("light", defaultDark = true))
        assertEquals(ThemeMode.DARK, ThemeRules.parseStored("DARK", defaultDark = false))
        assertEquals(ThemeMode.SYSTEM, ThemeRules.parseStored("system", defaultDark = true))
        assertEquals(ThemeMode.SYSTEM, ThemeRules.parseStored("auto", defaultDark = false))
        assertEquals(ThemeMode.DARK, ThemeRules.parseStored(null, defaultDark = true))
        assertEquals(ThemeMode.LIGHT, ThemeRules.parseStored("", defaultDark = false))
        assertEquals(ThemeMode.LIGHT, ThemeRules.parseStored("unknown", defaultDark = false))
    }

    @Test
    fun systemFollowsPhone() {
        assertTrue(ThemeRules.isDark(ThemeMode.DARK, systemDark = false))
        assertFalse(ThemeRules.isDark(ThemeMode.LIGHT, systemDark = true))
        assertTrue(ThemeRules.isDark(ThemeMode.SYSTEM, systemDark = true))
        assertFalse(ThemeRules.isDark(ThemeMode.SYSTEM, systemDark = false))
    }

    @Test
    fun statusMentionsSystem() {
        assertTrue(ThemeRules.statusLine(ThemeMode.SYSTEM).contains("системе"))
        assertTrue(ThemeRules.statusLine(ThemeMode.DARK).contains("тёмная"))
        assertTrue(ThemeRules.statusLine(ThemeMode.LIGHT).contains("светлая"))
        assertEquals("Тема системы", ThemeRules.signedOutContentDescription(ThemeMode.SYSTEM))
        assertEquals("Светлая тема", ThemeRules.signedOutContentDescription(ThemeMode.DARK))
        assertEquals(ThemeRules.SYSTEM_LABEL, "Система")
    }
}
