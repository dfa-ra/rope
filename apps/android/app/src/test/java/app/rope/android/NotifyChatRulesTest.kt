package app.rope.android

import app.rope.android.data.ChatIds
import app.rope.android.data.LocalStore
import app.rope.android.data.NotifyChatRules
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotifyChatRulesTest {
    @Test
    fun inheritStoreAndVersion() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, BuildConfig.VERSION_CODE)
        assertEquals("app.rope.android.extra.CHAT_ID", NotifyChatRules.EXTRA_CHAT_ID)
    }

    @Test
    fun chatIdRejectsBlankAndControl() {
        assertNull(NotifyChatRules.chatId(null))
        assertNull(NotifyChatRules.chatId("  "))
        assertNull(NotifyChatRules.chatId("null"))
        assertNull(NotifyChatRules.chatId("ab\ncd"))
        assertNull(NotifyChatRules.chatId("ab\rcd"))
        assertNull(NotifyChatRules.chatId("ab\u0000cd"))
        assertNull(NotifyChatRules.chatId("x".repeat(NotifyChatRules.MAX_ID + 1)))
        assertEquals("peer-1", NotifyChatRules.chatId("  peer-1  "))
        assertEquals(ChatIds.group("g9"), NotifyChatRules.chatId(ChatIds.group("g9")))
        assertEquals(SavedMessagesRules.ID, NotifyChatRules.chatId(SavedMessagesRules.ID))
    }

    @Test
    fun tapUriAndRequestCodeArePerChat() {
        val a = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        val b = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
        assertEquals("rope-notify://chat/$a", NotifyChatRules.tapUri(a))
        assertNotEquals(NotifyChatRules.requestCode(a), NotifyChatRules.requestCode(b))
        assertNotEquals(1, NotifyChatRules.requestCode(a))
        assertNotEquals(2, NotifyChatRules.requestCode(a))
        assertNull(NotifyChatRules.tapUri("id\nbad"))
        assertTrue(NotifyChatRules.canOpen(signedIn = true, incoming = a))
        assertFalse(NotifyChatRules.canOpen(signedIn = false, incoming = a))
        assertFalse(NotifyChatRules.canOpen(signedIn = true, incoming = "x\ny"))
        assertTrue(NotifyChatRules.alreadyShowing(a, a))
        assertFalse(NotifyChatRules.alreadyShowing("other", a))
        assertTrue(NotifyChatRules.parentChats(onChat = false, onChats = false, onArchive = false))
        assertFalse(NotifyChatRules.parentChats(onChat = true, onChats = false, onArchive = false))
        assertFalse(NotifyChatRules.parentChats(onChat = false, onChats = true, onArchive = false))
        assertFalse(NotifyChatRules.parentChats(onChat = false, onChats = false, onArchive = true))
    }
}
