package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.CopySpanRules
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.QuoteSpan
import app.rope.android.data.QuoteSpanRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CopySpanRulesTest {
    @Test
    fun inheritStoreAndVersion() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, BuildConfig.VERSION_CODE)
        assertEquals("Скопировано", CopySpanRules.NOTICE)
        assertEquals("Выделите фрагмент", CopySpanRules.HINT)
        assertFalse(CopySpanRules.NOTICE.contains("FCM", ignoreCase = true))
        assertFalse(CopySpanRules.HINT.contains("FCM", ignoreCase = true))
    }

    @Test
    fun copiesPartialAndFullBodyUnlikeQuotePacked() {
        val msg = text("привет мир")
        assertTrue(CopySpanRules.canCopy(msg))
        assertEquals("привет", CopySpanRules.clip(msg, 0, 6))
        assertEquals("привет мир", CopySpanRules.clip(msg, 0, msg.text.length))
        assertEquals("привет мир", CopySpanRules.clipOrAll(msg, null))
        assertEquals("мир", CopySpanRules.clipOrAll(msg, QuoteSpan(7, 10)))
        assertNull(QuoteSpanRules.packed(msg.text, QuoteSpan(0, msg.text.length)))
        assertNull(CopySpanRules.clip(msg, 3, 3))
        assertNull(CopySpanRules.clip(msg, 8, 2))
        val nl = text("hello\r\nworld")
        assertEquals("hello\r\nworld", CopySpanRules.clip(nl, 0, nl.text.length))
        assertEquals("hello", CopySpanRules.clip(nl, 0, 5))
    }

    @Test
    fun rejectsMediaDeletedBlankAndSnapsSurrogates() {
        val body = text("hello world")
        assertFalse(CopySpanRules.canCopy(body.copy(kind = MessageKind.IMAGE, text = "Фото")))
        assertFalse(CopySpanRules.canCopy(body.copy(kind = MessageKind.VOICE, text = "Голос")))
        assertFalse(CopySpanRules.canCopy(body.copy(deleted = true)))
        assertFalse(CopySpanRules.canCopy(body.copy(text = "  ")))
        assertNull(CopySpanRules.clip(body.copy(deleted = true), 0, 5))
        assertTrue(CopySpanRules.canCopy(body.copy(kind = MessageKind.GROUP_TEXT)))
        val emoji = text("a😀b")
        val span = CopySpanRules.clamp(emoji.text, 1, 3)!!
        assertEquals("😀", CopySpanRules.excerpt(emoji.text, span))
        assertEquals("😀", CopySpanRules.clip(emoji, 1, 3))
        val mid = CopySpanRules.clamp(emoji.text, 2, 3)!!
        assertFalse(emoji.text[mid.start].isLowSurrogate())
    }

    private fun text(body: String) = ChatMessage(
        id = "m",
        peerDeviceId = "p",
        outgoing = false,
        text = body,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        kind = MessageKind.TEXT,
    )
}
