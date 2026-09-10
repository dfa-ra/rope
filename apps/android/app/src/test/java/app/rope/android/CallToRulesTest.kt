package app.rope.android

import app.rope.android.data.CallToRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CallToRulesTest {
    @Test
    fun parseRejectsControlBeforeTrim() {
        val id = "a".repeat(64)
        assertEquals(id, CallToRules.parse(id))
        assertEquals(id, CallToRules.parse(id.uppercase()))
        assertNull(CallToRules.parse("\n$id"))
        assertNull(CallToRules.parse("$id\r"))
        assertNull(CallToRules.parse("$id\u0000"))
        assertNull(CallToRules.parse(" $id"))
        assertNull(CallToRules.parse(""))
        assertNull(CallToRules.parse(null))
    }
}
