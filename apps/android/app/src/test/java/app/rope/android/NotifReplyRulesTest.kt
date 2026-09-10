package app.rope.android

import app.rope.android.data.ChatIds
import app.rope.android.data.LocalStore
import app.rope.android.data.NotifReplyRules
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotifReplyRulesTest {
    @Test
    fun inheritStoreAndVersion() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, BuildConfig.VERSION_CODE)
        assertEquals("Ответить", NotifReplyRules.LABEL)
        assertEquals("app.rope.android.NOTIFY_REPLY", NotifReplyRules.ACTION)
        assertEquals("app.rope.android.extra.CHAT_ID", NotifReplyRules.EXTRA_CHAT_ID)
        assertEquals("app.rope.android.remote.REPLY", NotifReplyRules.REMOTE_KEY)
    }

    @Test
    fun chatIdAndTextRejectEmptyAndControl() {
        assertNull(NotifReplyRules.chatId(null))
        assertNull(NotifReplyRules.chatId("  "))
        assertNull(NotifReplyRules.chatId("null"))
        assertNull(NotifReplyRules.chatId("ab\ncd"))
        assertNull(NotifReplyRules.chatId("ab\rcd"))
        assertNull(NotifReplyRules.chatId("ab\u0000cd"))
        assertNull(NotifReplyRules.chatId("x".repeat(NotifReplyRules.MAX_ID + 1)))
        assertEquals("peer-1", NotifReplyRules.chatId("  peer-1  "))
        assertEquals(ChatIds.group("g9"), NotifReplyRules.chatId(ChatIds.group("g9")))
        assertNull(NotifReplyRules.text(null))
        assertNull(NotifReplyRules.text("  "))
        assertNull(NotifReplyRules.text("hi\nthere"))
        assertNull(NotifReplyRules.text("hi\rthere"))
        assertNull(NotifReplyRules.text("hi\u0000"))
        assertEquals("привет", NotifReplyRules.text("  привет  "))
        val long = "я".repeat(NotifReplyRules.MAX_TEXT + 8)
        assertEquals(NotifReplyRules.MAX_TEXT, NotifReplyRules.text(long)!!.length)
        assertTrue(NotifReplyRules.allowsReply("peer-1"))
        assertTrue(NotifReplyRules.allowsReply(ChatIds.group("g9")))
        assertFalse(NotifReplyRules.allowsReply(SavedMessagesRules.ID))
        assertFalse(NotifReplyRules.allowsReply("id\nbad"))
        val a = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        val b = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
        assertNotEquals(NotifReplyRules.requestCode(a), NotifReplyRules.requestCode(b))
        assertNotEquals(1, NotifReplyRules.requestCode(a))
        assertNotEquals(2, NotifReplyRules.requestCode(a))
    }
}
