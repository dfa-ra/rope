package app.rope.android

import app.rope.android.data.DiceRules
import app.rope.android.data.LocalStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiceRulesTest {
    @Test
    fun labelsAndVersion() {
        assertEquals("Кубик", DiceRules.LABEL)
        assertEquals(listOf("🎲", "🎯", "🏀", "⚽", "🎳"), DiceRules.KINDS.map { it.emoji })
        assertEquals(listOf(6, 6, 5, 5, 6), DiceRules.KINDS.map { it.faces })
        assertFalse(DiceRules.LABEL.contains('\n'))
        DiceRules.KINDS.forEach { kind ->
            assertFalse(kind.emoji.contains('\n'))
            assertFalse(kind.label.contains('\n'))
        }
        assertEquals("0.3.52", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(76, BuildConfig.VERSION_CODE)
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun textAndParseClampFaces() {
        val cube = DiceRules.kind("🎲")!!
        assertEquals("🎲 4", DiceRules.text(cube, 4))
        assertEquals(cube to 4, DiceRules.parse("🎲 4"))
        assertTrue(DiceRules.isDice("🎲 4"))
        assertNull(DiceRules.text(cube, 0))
        assertNull(DiceRules.text(cube, 7))
        assertNull(DiceRules.parse("🎲 7"))
        val ball = DiceRules.kind("⚽")!!
        assertEquals("⚽ 5", DiceRules.text(ball, 5))
        assertNull(DiceRules.text(ball, 6))
        assertNull(DiceRules.parse("hello"))
        assertNull(DiceRules.parse("🎲\n4"))
        assertNull(DiceRules.kind("bad\nemoji"))
        assertNull(DiceRules.kind(null))
        assertFalse(DiceRules.isDice("https://www.openstreetmap.org/?mlat=1"))
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun allKindsRoundTrip() {
        DiceRules.KINDS.forEach { kind ->
            for (face in 1..kind.faces) {
                val text = DiceRules.text(kind, face)
                assertNotNull(text)
                assertEquals(kind to face, DiceRules.parse(text))
                assertFalse(text!!.contains('\n'))
                assertFalse(text.contains('\r'))
            }
        }
    }
}
