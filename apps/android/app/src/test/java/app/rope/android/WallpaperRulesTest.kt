package app.rope.android

import app.rope.android.data.WallpaperRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperRulesTest {
    @Test
    fun parseFallsBackToThemeAndRejectsUnknown() {
        assertEquals(WallpaperRules.DEFAULT, WallpaperRules.parse(null))
        assertEquals(WallpaperRules.DEFAULT, WallpaperRules.parse("  "))
        assertEquals(WallpaperRules.DEFAULT, WallpaperRules.parse("neon-logo"))
        assertEquals(WallpaperRules.SLATE, WallpaperRules.parse(" slate "))
        assertEquals(5, WallpaperRules.PRESETS.size)
        assertEquals("Тема", WallpaperRules.label(WallpaperRules.DEFAULT))
        assertEquals("Сланец", WallpaperRules.label(WallpaperRules.SLATE))
    }

    @Test
    fun defaultUsesThemeBackgroundOthersHaveDarkAndLight() {
        assertNull(WallpaperRules.argb(WallpaperRules.DEFAULT, dark = true))
        assertNull(WallpaperRules.argb(WallpaperRules.DEFAULT, dark = false))
        val dark = WallpaperRules.argb(WallpaperRules.GRAPHITE, dark = true)
        val light = WallpaperRules.argb(WallpaperRules.GRAPHITE, dark = false)
        assertNotNull(dark)
        assertNotNull(light)
        assertNotEquals(dark, light)
        WallpaperRules.PRESETS.filter { it.id != WallpaperRules.DEFAULT }.forEach { preset ->
            assertNotNull(WallpaperRules.argb(preset.id, dark = true))
            assertNotNull(WallpaperRules.argb(preset.id, dark = false))
        }
    }

    @Test
    fun hintKeepsLogoUntouchedAndNoFcm() {
        val hint = WallpaperRules.hint().lowercase()
        assertTrue(hint.contains("логотип"))
        assertTrue(!hint.contains("fcm"))
    }
}
