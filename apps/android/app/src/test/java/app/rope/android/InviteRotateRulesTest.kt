package app.rope.android

import app.rope.android.data.InviteRotateRules
import app.rope.android.data.LocalStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InviteRotateRulesTest {
    @Test
    fun labelsAndVersion() {
        assertEquals("Новая ссылка", InviteRotateRules.LABEL)
        assertTrue(InviteRotateRules.HINT.contains("TTL"))
        assertFalse(InviteRotateRules.LABEL.contains('\n'))
        assertFalse(InviteRotateRules.HINT.contains("FCM", ignoreCase = true))
        assertFalse(InviteRotateRules.LABEL.contains("скопир", ignoreCase = true))
        assertFalse(InviteRotateRules.LABEL.contains("Поделиться"))
        assertTrue(InviteRotateRules.enabled(busy = false))
        assertFalse(InviteRotateRules.enabled(busy = true))
        assertEquals("0.3.52", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(76, BuildConfig.VERSION_CODE)
        assertEquals(6, LocalStore.VERSION)
    }
}
