package app.rope.android

import app.rope.android.data.JoinClipboardRules
import app.rope.android.data.LocalStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JoinClipboardRulesTest {
    @Test
    fun acceptJoinUrlAndVersion() {
        assertEquals("Вставить", JoinClipboardRules.LABEL)
        assertEquals("rope://join", JoinClipboardRules.PREFIX)
        assertEquals("В буфере нет ссылки", JoinClipboardRules.EMPTY)
        assertFalse(JoinClipboardRules.LABEL.contains('\n'))
        assertFalse(JoinClipboardRules.EMPTY.contains("SSH", ignoreCase = true))
        assertFalse(JoinClipboardRules.LABEL.contains("FCM", ignoreCase = true))
        val ok = "rope://join?v=1&host=vps.example&port=8443&sid=ab&fp=cd&tok=ef"
        assertEquals(ok, JoinClipboardRules.accept("  $ok  "))
        assertEquals("rope://join", JoinClipboardRules.accept("rope://join"))
        assertNull(JoinClipboardRules.accept(null))
        assertNull(JoinClipboardRules.accept(""))
        assertNull(JoinClipboardRules.accept("https://example/join"))
        assertNull(JoinClipboardRules.accept("rope://join?tok=1\n"))
        assertNull(JoinClipboardRules.accept("ROPE://join?v=1"))
        assertEquals("0.3.52", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(76, BuildConfig.VERSION_CODE)
        assertEquals(6, LocalStore.VERSION)
        assertTrue(JoinClipboardRules.EMPTY.contains("буфере"))
    }
}
