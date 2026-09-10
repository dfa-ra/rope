package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.PrivacyCallsRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyCallsRulesTest {
    @Test
    fun labelsMuteAndVersion() {
        assertEquals("Звонки", PrivacyCallsRules.TITLE)
        assertEquals("Не принимать входящие", PrivacyCallsRules.LABEL)
        assertTrue(PrivacyCallsRules.HINT.contains("отклоняются"))
        assertTrue(PrivacyCallsRules.HINT.contains("сворачивание"))
        assertFalse(PrivacyCallsRules.LABEL.contains('\n'))
        assertFalse(PrivacyCallsRules.HINT.contains("FCM", ignoreCase = true))
        assertFalse(PrivacyCallsRules.parse(null))
        assertFalse(PrivacyCallsRules.parse("0"))
        assertFalse(PrivacyCallsRules.parse("1\n"))
        assertTrue(PrivacyCallsRules.parse("1"))
        assertEquals("1", PrivacyCallsRules.persist(true))
        assertEquals("0", PrivacyCallsRules.persist(false))
        assertTrue(PrivacyCallsRules.shouldAutoReject(true))
        assertFalse(PrivacyCallsRules.shouldAutoReject(false))
        assertEquals("0.3.52", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(76, BuildConfig.VERSION_CODE)
        assertEquals(6, LocalStore.VERSION)
    }
}
