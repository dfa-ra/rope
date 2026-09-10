package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.ShareInviteRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareInviteRulesTest {
    @Test
    fun acceptJoinUrlRejectsCrLfNul() {
        val ok = "rope://join?v=1&host=h&port=8443&sid=s&fp=ab&tok=t"
        assertEquals(ok, ShareInviteRules.accept("  $ok  "))
        assertTrue(ShareInviteRules.enabled(ok))
        assertNull(ShareInviteRules.accept("rope://join?"))
        assertNull(ShareInviteRules.accept("https://example/join?tok=t"))
        assertNull(ShareInviteRules.accept("rope://join?\nfoo"))
        assertNull(ShareInviteRules.accept("rope://join?\rfoo"))
        assertNull(ShareInviteRules.accept("rope://join?\u0000tok=t"))
        assertFalse(ShareInviteRules.enabled(""))
        assertEquals("Поделиться", ShareInviteRules.LABEL)
        assertEquals("Поделиться приглашением", ShareInviteRules.CHOOSER)
        assertEquals("android.intent.action.SEND", ShareInviteRules.SEND_ACTION)
        assertEquals("android.intent.extra.TEXT", ShareInviteRules.EXTRA_TEXT)
        assertEquals("text/plain", ShareInviteRules.MIME)
        assertEquals(6, LocalStore.VERSION)
    }
}
