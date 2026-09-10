package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.SaveInviteQrRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveInviteQrRulesTest {
    @Test
    fun gallerySaveRejectsCrLfAndVersion() {
        val ok = "rope://join?tok=abc"
        assertEquals(ok, SaveInviteQrRules.accept("  $ok  "))
        assertTrue(SaveInviteQrRules.enabled(ok))
        assertNull(SaveInviteQrRules.accept("rope://join?"))
        assertNull(SaveInviteQrRules.accept("https://example/join?tok=t"))
        assertNull(SaveInviteQrRules.accept("rope://join?\nfoo"))
        assertNull(SaveInviteQrRules.accept("rope://join?\rfoo"))
        assertNull(SaveInviteQrRules.accept("rope://join?\u0000tok=t"))
        assertFalse(SaveInviteQrRules.enabled(""))
        assertEquals("Сохранить QR", SaveInviteQrRules.LABEL)
        assertEquals("QR в галерее", SaveInviteQrRules.NOTICE)
        assertEquals("image/png", SaveInviteQrRules.MIME)
        assertEquals("rope-invite-qr.png", SaveInviteQrRules.displayName())
        assertFalse(SaveInviteQrRules.displayName().contains('/'))
        assertFalse(SaveInviteQrRules.FILE.contains('\n'))
        assertFalse(SaveInviteQrRules.LABEL.contains("Поделиться"))
        assertFalse(SaveInviteQrRules.LABEL.contains("скопир", ignoreCase = true))
        assertFalse(SaveInviteQrRules.LABEL.contains("FCM", ignoreCase = true))
        assertEquals("0.3.52", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(76, BuildConfig.VERSION_CODE)
        assertEquals(6, LocalStore.VERSION)
    }
}
