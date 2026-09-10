package app.rope.android

import app.rope.android.data.ChatIds
import app.rope.android.data.ChatPrefs
import app.rope.android.data.LocalStore
import app.rope.android.data.NotifMuteRules
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotifMuteRulesTest {
    @Test
    fun inheritStoreAndVersion() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.52", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(76, BuildConfig.VERSION_CODE)
        assertEquals("Без звука", NotifMuteRules.LABEL)
        assertEquals("app.rope.android.NOTIFY_MUTE", NotifMuteRules.ACTION)
        assertEquals("app.rope.android.extra.MUTE_CHAT_ID", NotifMuteRules.EXTRA_CHAT_ID)
        assertFalse(NotifMuteRules.LABEL.contains('\n'))
        assertFalse(NotifMuteRules.ACTION.contains("NOTIFY_READ"))
        assertFalse(NotifMuteRules.ACTION.contains("NOTIFY_REPLY"))
    }

    @Test
    fun chatIdAndApplyMute() {
        assertNull(NotifMuteRules.chatId(null))
        assertNull(NotifMuteRules.chatId("  "))
        assertNull(NotifMuteRules.chatId("null"))
        assertNull(NotifMuteRules.chatId("ab\ncd"))
        assertNull(NotifMuteRules.chatId("ab\rcd"))
        assertNull(NotifMuteRules.chatId("ab\u0000cd"))
        assertNull(NotifMuteRules.chatId("x".repeat(NotifMuteRules.MAX_ID + 1)))
        assertEquals("peer-1", NotifMuteRules.chatId("  peer-1  "))
        assertEquals(ChatIds.group("g9"), NotifMuteRules.chatId(ChatIds.group("g9")))
        assertTrue(NotifMuteRules.allows("peer-1"))
        assertTrue(NotifMuteRules.allows(ChatIds.group("g9")))
        assertFalse(NotifMuteRules.allows(SavedMessagesRules.ID))
        assertFalse(NotifMuteRules.allows("id\nbad"))
        val a = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        val b = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
        assertNotEquals(NotifMuteRules.requestCode(a), NotifMuteRules.requestCode(b))
        assertNotEquals(1, NotifMuteRules.requestCode(a))
        assertNotEquals(2, NotifMuteRules.requestCode(a))
        val muted = NotifMuteRules.apply(ChatPrefs(unread = 4, muted = false, lastReadMs = 1L))
        assertTrue(muted.muted)
        assertEquals(4, muted.unread)
        assertEquals(1L, muted.lastReadMs)
        assertTrue(NotifMuteRules.apply(ChatPrefs(muted = true)).muted)
        assertEquals(6, LocalStore.VERSION)
    }
}
