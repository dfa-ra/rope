package app.rope.android

import app.rope.android.data.CopyIdRules
import app.rope.android.data.LocalStore
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CopyIdRulesTest {
    @Test
    fun canCopyPeersAndGroupsNotSaved() {
        assertTrue(CopyIdRules.canCopy("peer-1"))
        assertTrue(CopyIdRules.canCopy("g-uuid"))
        assertFalse(CopyIdRules.canCopy(null))
        assertFalse(CopyIdRules.canCopy(""))
        assertFalse(CopyIdRules.canCopy("  "))
        assertFalse(CopyIdRules.canCopy(SavedMessagesRules.ID))
    }

    @Test
    fun clipTrimsAndPreviewEllipsizes() {
        assertEquals("peer-1", CopyIdRules.clip("  peer-1  "))
        assertNull(CopyIdRules.clip("  "))
        assertNull(CopyIdRules.clip(SavedMessagesRules.ID))
        assertEquals("short", CopyIdRules.preview("short"))
        assertEquals("abcdefghijkl", CopyIdRules.preview("abcdefghijkl"))
        assertEquals("abcdefgh…", CopyIdRules.preview("abcdefghijklmnop"))
        assertEquals("", CopyIdRules.preview(null))
        assertEquals("Скопировать ID", CopyIdRules.ACTION)
        assertEquals(12, CopyIdRules.PREVIEW_MAX)
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
    }
}
