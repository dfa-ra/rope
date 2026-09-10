package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.CopyLinkRules
import app.rope.android.data.LinkPreviewRules
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CopyLinkRulesTest {
    @Test
    fun firstHttpsFromTextCanonicalizes() {
        val msg = msg("смотри https://Example.COM/a и ещё")
        assertEquals("https://example.com/a", CopyLinkRules.url(msg))
        assertTrue(CopyLinkRules.canCopy(msg))
    }

    @Test
    fun packedPreviewBeatsMissingTextLink() {
        val preview = LinkPreviewRules.parse("https://www.example.com/photo")
        val photo = msg("", kind = MessageKind.IMAGE).copy(linkPreview = preview)
        assertEquals("https://www.example.com/photo", CopyLinkRules.url(photo))
        assertTrue(CopyLinkRules.canCopy(photo))
    }

    @Test
    fun rejectsHttpJavascriptCrLfDeletedAndBlank() {
        assertNull(CopyLinkRules.url(msg("http://example.com/a")))
        assertNull(CopyLinkRules.url(msg("javascript:alert(1)")))
        assertEquals(
            "https://example.com/a",
            CopyLinkRules.url(msg("https://example.com/a\nhttps://evil.example")),
        )
        assertNull(CopyLinkRules.sanitize("https://example.com/a\r\nHost: evil"))
        assertNull(CopyLinkRules.sanitize("https://example.com/a\u0000.js"))
        assertNull(CopyLinkRules.sanitize("https://127.0.0.1/x"))
        assertNull(CopyLinkRules.sanitize("intent://scan/#Intent;end"))
        assertFalse(CopyLinkRules.canCopy(msg("https://example.com/a", deleted = true)))
        assertFalse(CopyLinkRules.canCopy(msg("просто текст")))
        assertEquals("Копировать ссылку", CopyLinkRules.ACTION)
        assertEquals(6, LocalStore.VERSION)
    }

    private fun msg(
        text: String,
        kind: MessageKind = MessageKind.TEXT,
        deleted: Boolean = false,
    ) = ChatMessage(
        id = "m1",
        peerDeviceId = "p",
        outgoing = false,
        text = text,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        kind = kind,
        deleted = deleted,
    )
}
