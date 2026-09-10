package app.rope.android

import app.rope.android.data.InviteCopyRules
import app.rope.android.data.LocalStore
import app.rope.android.protocol.InviteCodec
import app.rope.android.protocol.InviteLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InviteCopyRulesTest {
    @Test
    fun inheritStoreAndCopyOnlyRopeJoin() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("Скопировать ссылку", InviteCopyRules.LABEL)
        val url = InviteCodec.build(
            InviteLink(1, "203.0.113.10", 8443, "sid", "deadbeef", "tok-1", "Ada"),
        )
        assertEquals(url, InviteCopyRules.accept("  $url  "))
        assertTrue(InviteCopyRules.enabled(url))
        assertNull(InviteCopyRules.accept("https://example.com"))
        assertNull(InviteCopyRules.accept("rope://join"))
        assertNull(InviteCopyRules.accept("rope://join?\nbad"))
        assertNull(InviteCopyRules.accept("rope://join?\rbad"))
        assertNull(InviteCopyRules.accept("rope://join?\u0000bad"))
        assertFalse(InviteCopyRules.enabled(""))
        assertFalse(InviteCopyRules.LABEL.contains("FCM", ignoreCase = true))
    }
}
