package app.rope.android

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ThemePaletteTest {
    @Test
    fun darkSecondColorIsLightAndDarkGray() {
        assertEquals(Color(0xFFD4D4D8), RopeLightGray)
        assertEquals(Color(0xFF3F3F46), RopeDarkGray)
        assertEquals(Color(0xFF09090B), RopeDarkBg)
        assertEquals(Color(0xFFD4D4D8), RopeGrayLight)
        assertEquals(Color(0xFF3F3F46), RopeGrayDark)
    }

    @Test
    fun secondColorIsNotNeonCyan() {
        val neon = Color(0xFF00D4FF)
        assertNotEquals(neon, RopeLightGray)
        assertNotEquals(neon, RopeDarkGray)
        assertNotEquals(neon, RopeMidGray)
        assertNotEquals(neon, RopeNeon)
    }

    @Test
    fun darkPrimaryIsBlackAndWhite() {
        assertEquals(Color(0xFF09090B), RopeBlack)
        assertEquals(Color(0xFFFAFAFA), RopeWhite)
    }
}
