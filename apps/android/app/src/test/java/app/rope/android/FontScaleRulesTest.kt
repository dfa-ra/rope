package app.rope.android

import app.rope.android.data.FontScaleRules
import org.junit.Assert.assertEquals
import org.junit.Test

class FontScaleRulesTest {
    @Test
    fun parseFallsBackToNormal() {
        assertEquals(FontScaleRules.NORMAL, FontScaleRules.parse(null))
        assertEquals(FontScaleRules.NORMAL, FontScaleRules.parse("  "))
        assertEquals(FontScaleRules.NORMAL, FontScaleRules.parse("huge"))
        assertEquals(FontScaleRules.SMALL, FontScaleRules.parse(" S "))
        assertEquals(FontScaleRules.XL, FontScaleRules.parse("xl"))
    }

    @Test
    fun factorMatchesPreset() {
        assertEquals(0.85f, FontScaleRules.factor("s"), 0.001f)
        assertEquals(1.0f, FontScaleRules.factor(null), 0.001f)
        assertEquals(1.15f, FontScaleRules.factor("l"), 0.001f)
        assertEquals(1.3f, FontScaleRules.factor("xl"), 0.001f)
        assertEquals(1.0f, FontScaleRules.factor("nope"), 0.001f)
    }
}
