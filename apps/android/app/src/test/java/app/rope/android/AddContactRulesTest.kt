package app.rope.android

import app.rope.android.data.AddContactRules
import app.rope.android.data.LocalStore
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddContactRulesTest {
    @Test
    fun localStoreStaysV6() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, BuildConfig.VERSION_CODE)
    }

    @Test
    fun hiddenForSavedAndBlank() {
        assertTrue(AddContactRules.show(saved = false, name = "Ann"))
        assertFalse(AddContactRules.show(saved = true, name = "Ann"))
        assertFalse(AddContactRules.show(saved = false, name = "  "))
        assertFalse(AddContactRules.show(saved = false, name = null))
        assertEquals("Ann", AddContactRules.name("  Ann "))
        assertEquals("В контакты", AddContactRules.LABEL)
        assertEquals("name", AddContactRules.EXTRA_NAME)
        assertTrue(AddContactRules.MIME.contains("raw_contact"))
        assertTrue(SavedMessagesRules.isSaved(SavedMessagesRules.ID))
        assertFalse(AddContactRules.show(SavedMessagesRules.isSaved(SavedMessagesRules.ID), "Ann"))
    }
}
