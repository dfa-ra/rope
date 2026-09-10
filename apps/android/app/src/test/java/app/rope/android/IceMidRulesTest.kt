package app.rope.android

import app.rope.android.data.IceMidRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IceMidRulesTest {
    @Test
    fun localMidRejectsControlBeforeTrim() {
        assertEquals("0", IceMidRules.localMid(null))
        assertEquals("0", IceMidRules.localMid(""))
        assertEquals("0", IceMidRules.localMid("  "))
        assertEquals("0", IceMidRules.localMid("0"))
        assertEquals("audio", IceMidRules.localMid(" audio "))
        assertNull(IceMidRules.localMid("\n0"))
        assertNull(IceMidRules.localMid("0\n"))
        assertNull(IceMidRules.localMid("audio\r"))
        assertNull(IceMidRules.localMid("0\u0000"))
    }
}
