package app.rope.android

import app.rope.android.data.BioRules
import app.rope.android.data.LocalStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BioRulesTest {
    @Test
    fun stripsCrLfNulAndCapsLength() {
        assertEquals("", BioRules.sanitize(null))
        assertEquals("", BioRules.sanitize(""))
        assertEquals("привет", BioRules.sanitize("привет"))
        assertEquals("привет", BioRules.sanitize("при\nвет"))
        assertEquals("привет", BioRules.sanitize("при\rвет"))
        assertEquals("привет", BioRules.sanitize("при\u0000вет"))
        val long = "я".repeat(BioRules.MAX + 9)
        assertEquals(BioRules.MAX, BioRules.sanitize(long).length)
        assertEquals(0, BioRules.remaining("x".repeat(BioRules.MAX)))
        assertEquals(BioRules.MAX, BioRules.remaining(""))
        assertEquals("О себе", BioRules.TITLE)
        assertTrue(BioRules.hint().contains("ник"))
        assertEquals(6, LocalStore.VERSION)
    }
}
