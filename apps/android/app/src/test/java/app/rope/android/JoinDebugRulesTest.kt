package app.rope.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JoinDebugRulesTest {
    @Test
    fun httpJoinOnlyOnDebugBuilds() {
        assertTrue(JoinDebugRules.showHttpJoin(true))
        assertFalse(JoinDebugRules.showHttpJoin(false))
    }
}
