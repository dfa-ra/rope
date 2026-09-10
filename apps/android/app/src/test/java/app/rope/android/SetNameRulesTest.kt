package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.SetNameRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetNameRulesTest {
    @Test
    fun saveOnlyWhenValidAndChanged() {
        assertEquals("Имя", SetNameRules.LABEL)
        assertEquals("Сохранить", SetNameRules.SAVE)
        assertTrue(SetNameRules.HINT.contains("телефоне"))
        assertTrue(SetNameRules.HINT.contains("ник"))
        assertFalse(SetNameRules.LABEL.contains('\n'))
        assertFalse(SetNameRules.HINT.contains("FCM", ignoreCase = true))
        assertTrue(SetNameRules.canSave("ann", "bob"))
        assertTrue(SetNameRules.canSave("  ann  ", "bob"))
        assertFalse(SetNameRules.canSave("ann", "ann"))
        assertFalse(SetNameRules.canSave("  ann  ", "ann"))
        assertFalse(SetNameRules.canSave("a", "bob"))
        assertFalse(SetNameRules.canSave("has space", "bob"))
        assertFalse(SetNameRules.canSave("", "bob"))
        assertEquals("0.3.52", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(76, BuildConfig.VERSION_CODE)
        assertEquals(6, LocalStore.VERSION)
    }
}
