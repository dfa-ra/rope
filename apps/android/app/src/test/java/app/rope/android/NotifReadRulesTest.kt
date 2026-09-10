package app.rope.android

import app.rope.android.data.ChatIds
import app.rope.android.data.ChatPrefs
import app.rope.android.data.LocalStore
import app.rope.android.data.NotifReadRules
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotifReadRulesTest {
    @Test
    fun inheritStoreAndVersion() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, BuildConfig.VERSION_CODE)
        assertEquals("Прочитать", NotifReadRules.LABEL)
        assertEquals("app.rope.android.NOTIFY_READ", NotifReadRules.ACTION)
        assertEquals("app.rope.android.extra.CHAT_ID", NotifReadRules.EXTRA_CHAT_ID)
        assertFalse(NotifReadRules.LABEL.contains('\n'))
    }

    @Test
    fun chatIdRejectsEmptyAndControl() {
        assertNull(NotifReadRules.chatId(null))
        assertNull(NotifReadRules.chatId("  "))
        assertNull(NotifReadRules.chatId("null"))
        assertNull(NotifReadRules.chatId("ab\ncd"))
        assertNull(NotifReadRules.chatId("ab\rcd"))
        assertNull(NotifReadRules.chatId("ab\u0000cd"))
        assertNull(NotifReadRules.chatId("x".repeat(NotifReadRules.MAX_ID + 1)))
        assertEquals("peer-1", NotifReadRules.chatId("  peer-1  "))
        assertEquals(ChatIds.group("g9"), NotifReadRules.chatId(ChatIds.group("g9")))
        assertTrue(NotifReadRules.allows("peer-1"))
        assertTrue(NotifReadRules.allows(ChatIds.group("g9")))
        assertFalse(NotifReadRules.allows(SavedMessagesRules.ID))
        assertFalse(NotifReadRules.allows("id\nbad"))
        val a = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        val b = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
        assertNotEquals(NotifReadRules.requestCode(a), NotifReadRules.requestCode(b))
        assertNotEquals(1, NotifReadRules.requestCode(a))
        assertNotEquals(2, NotifReadRules.requestCode(a))
        val marked = NotifReadRules.apply(ChatPrefs(unread = 4, lastReadMs = 1L), nowMs = 99L)
        assertEquals(0, marked.unread)
        assertEquals(99L, marked.lastReadMs)
        assertEquals(6, LocalStore.VERSION)
    }
}
