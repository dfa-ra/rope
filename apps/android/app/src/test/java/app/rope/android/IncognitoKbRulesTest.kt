package app.rope.android

import app.rope.android.data.IncognitoKbRules
import app.rope.android.data.LocalStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IncognitoKbRulesTest {
    @Test
    fun kvDefaultsOff() {
        assertFalse(IncognitoKbRules.enabledFromKv(null))
        assertFalse(IncognitoKbRules.enabledFromKv("0"))
        assertTrue(IncognitoKbRules.enabledFromKv("1"))
    }

    @Test
    fun autoCorrectOffOnlyWhenIncognito() {
        assertTrue(IncognitoKbRules.autoCorrect(incognito = false))
        assertFalse(IncognitoKbRules.autoCorrect(incognito = true))
        assertTrue(IncognitoKbRules.hint().contains("клавиатур"))
        assertTrue(IncognitoKbRules.hint().contains("FCM"))
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
    }
}
