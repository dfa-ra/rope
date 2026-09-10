package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageSearch
import app.rope.android.data.MessageSearchKind
import app.rope.android.data.MessageSearchKindRules
import app.rope.android.data.MessageStatus
import app.rope.android.data.PackedLinkPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageSearchKindRulesTest {
    @Test
    fun chipsArePhotoFileLink() {
        assertEquals(
            listOf(MessageSearchKind.PHOTO, MessageSearchKind.FILE, MessageSearchKind.LINK),
            MessageSearchKindRules.chips(),
        )
        assertEquals("Фото", MessageSearchKindRules.PHOTO)
        assertEquals("Файлы", MessageSearchKindRules.FILE)
        assertEquals("Ссылки", MessageSearchKindRules.LINK)
        assertEquals("Фото", MessageSearchKindRules.label(MessageSearchKind.PHOTO))
        assertEquals(MessageSearchKind.ALL, MessageSearchKindRules.toggle(MessageSearchKind.PHOTO, MessageSearchKind.PHOTO))
        assertEquals(MessageSearchKind.FILE, MessageSearchKindRules.toggle(MessageSearchKind.ALL, MessageSearchKind.FILE))
    }

    @Test
    fun kindFilterIgnoresUnrelatedRows() {
        val photo = msg("p", "снимок", MessageKind.IMAGE)
        val video = msg("v", "клип", MessageKind.VIDEO)
        val file = msg("f", "акт.pdf", MessageKind.FILE)
        val link = msg("l", "смотри https://example.com/a")
        val text = msg("t", "просто текст")
        assertTrue(MessageSearch.matches(photo, "", MessageSearchKind.PHOTO))
        assertTrue(MessageSearch.matches(video, "", MessageSearchKind.PHOTO))
        assertFalse(MessageSearch.matches(file, "", MessageSearchKind.PHOTO))
        assertTrue(MessageSearch.matches(file, "", MessageSearchKind.FILE))
        assertFalse(MessageSearch.matches(photo, "", MessageSearchKind.FILE))
        assertTrue(MessageSearch.matches(link, "", MessageSearchKind.LINK))
        assertTrue(MessageSearch.matches(text.copy(linkPreview = PackedLinkPreview("https://a.example", "a.example", "A")), "", MessageSearchKind.LINK))
        assertFalse(MessageSearch.matches(text, "", MessageSearchKind.LINK))
        assertTrue(MessageSearch.matches(photo, "сним", MessageSearchKind.PHOTO))
        assertFalse(MessageSearch.matches(photo, "акт", MessageSearchKind.PHOTO))
        assertTrue(MessageSearch.matches(text, "текст"))
        assertTrue(MessageSearchKindRules.searching("", MessageSearchKind.PHOTO))
        assertFalse(MessageSearchKindRules.searching("", MessageSearchKind.ALL))
        assertEquals("Нет фото в этом чате.", MessageSearchKindRules.emptyBody(MessageSearchKind.PHOTO))
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
    }

    private fun msg(id: String, text: String, kind: MessageKind = MessageKind.TEXT) = ChatMessage(
        id = id,
        peerDeviceId = "p",
        outgoing = false,
        text = text,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        kind = kind,
    )
}
