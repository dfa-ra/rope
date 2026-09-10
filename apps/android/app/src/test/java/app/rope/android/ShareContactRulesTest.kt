package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.SavedMessagesRules
import app.rope.android.data.ShareContactRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareContactRulesTest {
    @Test
    fun canSharePeersNotSelfOrSaved() {
        assertTrue(ShareContactRules.canShare("peer-1", "me"))
        assertTrue(ShareContactRules.canShare("  peer-1  ", "me"))
        assertFalse(ShareContactRules.canShare(null, "me"))
        assertFalse(ShareContactRules.canShare("", "me"))
        assertFalse(ShareContactRules.canShare("  ", "me"))
        assertFalse(ShareContactRules.canShare("me", "me"))
        assertFalse(ShareContactRules.canShare(SavedMessagesRules.ID, "me"))
        assertTrue(ShareContactRules.canShare("peer-1", null))
        assertEquals("Поделиться", ShareContactRules.ACTION)
        assertEquals("Поделиться контактом", ShareContactRules.BANNER)
    }

    @Test
    fun canSendToOtherChatsNotSamePeer() {
        assertTrue(ShareContactRules.canSendTo("peer-2", "peer-1"))
        assertTrue(ShareContactRules.canSendTo(SavedMessagesRules.ID, "peer-1"))
        assertTrue(ShareContactRules.canSendTo("g-uuid", "peer-1"))
        assertFalse(ShareContactRules.canSendTo("peer-1", "peer-1"))
        assertFalse(ShareContactRules.canSendTo("  peer-1  ", "peer-1"))
        assertFalse(ShareContactRules.canSendTo("", "peer-1"))
        assertFalse(ShareContactRules.canSendTo("peer-2", ""))
        assertFalse(ShareContactRules.canSendTo(null, "peer-1"))
        assertEquals(ShareContactRules.REJECT, "Нельзя отправить контакт в этот чат.")
    }

    @Test
    fun bodyAndPreviewUseNameFallback() {
        assertEquals("Анна\ndev-1", ShareContactRules.body("  Анна  ", "dev-1"))
        assertEquals("контакт\ndev-1", ShareContactRules.body("  ", "dev-1"))
        assertEquals("контакт\ndev-1", ShareContactRules.body(null, "dev-1"))
        assertEquals("Анна", ShareContactRules.preview(" Анна "))
        assertEquals("контакт", ShareContactRules.preview(""))
        assertEquals("контакт", ShareContactRules.preview(null))
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
    }
}
