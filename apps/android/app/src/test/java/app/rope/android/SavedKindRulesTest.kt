package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.PackedLinkPreview
import app.rope.android.data.SavedKind
import app.rope.android.data.SavedKindRules
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedKindRulesTest {
    private fun msg(
        id: String,
        kind: MessageKind,
        text: String = "",
        deleted: Boolean = false,
        lp: PackedLinkPreview? = null,
    ) = ChatMessage(
        id = id,
        peerDeviceId = SavedMessagesRules.ID,
        outgoing = true,
        text = text,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        kind = kind,
        deleted = deleted,
        linkPreview = lp,
    )

    @Test
    fun showsOnlyOnSavedThread() {
        assertTrue(SavedKindRules.shows(SavedMessagesRules.ID, isGroup = false))
        assertFalse(SavedKindRules.shows("peer", isGroup = false))
        assertFalse(SavedKindRules.shows(SavedMessagesRules.ID, isGroup = true))
        assertEquals(listOf(SavedKind.ALL, SavedKind.PHOTO, SavedKind.FILE, SavedKind.LINK), SavedKindRules.chips())
        assertEquals("Все", SavedKindRules.label(SavedKind.ALL))
        assertEquals("Фото", SavedKindRules.label(SavedKind.PHOTO))
        assertEquals("Файлы", SavedKindRules.label(SavedKind.FILE))
        assertEquals("Ссылки", SavedKindRules.label(SavedKind.LINK))
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun photoFileLinkAndAll() {
        val photo = msg("1", MessageKind.IMAGE)
        val video = msg("2", MessageKind.VIDEO)
        val file = msg("3", MessageKind.FILE)
        val link = msg("4", MessageKind.TEXT, text = "см. https://example.com/a")
        val note = msg("5", MessageKind.TEXT, text = "заметка")
        val gone = msg("6", MessageKind.IMAGE, deleted = true)
        assertTrue(SavedKindRules.hits(photo, SavedKind.PHOTO))
        assertTrue(SavedKindRules.hits(video, SavedKind.PHOTO))
        assertFalse(SavedKindRules.hits(file, SavedKind.PHOTO))
        assertTrue(SavedKindRules.hits(file, SavedKind.FILE))
        assertTrue(SavedKindRules.hits(link, SavedKind.LINK))
        assertFalse(SavedKindRules.hits(note, SavedKind.LINK))
        assertTrue(SavedKindRules.hits(note, SavedKind.ALL))
        assertFalse(SavedKindRules.hits(gone, SavedKind.PHOTO))
        val all = listOf(photo, video, file, link, note, gone)
        assertEquals(listOf("1", "2"), SavedKindRules.apply(all, SavedKind.PHOTO).map { it.id })
        assertEquals(listOf("3"), SavedKindRules.apply(all, SavedKind.FILE).map { it.id })
        assertEquals(listOf("4"), SavedKindRules.apply(all, SavedKind.LINK).map { it.id })
        assertEquals(all.size, SavedKindRules.apply(all, SavedKind.ALL).size)
    }

    @Test
    fun packedPreviewCountsAsLink() {
        val lp = PackedLinkPreview(url = "https://example.com", host = "example.com", title = "t")
        val packed = msg("1", MessageKind.TEXT, text = "без url в тексте", lp = lp)
        assertTrue(SavedKindRules.hasLink(packed))
        assertTrue(SavedKindRules.hits(packed, SavedKind.LINK))
        assertFalse(SavedKindRules.hasLink(msg("2", MessageKind.TEXT, text = "просто текст")))
    }

    @Test
    fun kvAndEmptyCopy() {
        assertEquals(SavedKind.PHOTO, SavedKindRules.parse("photo"))
        assertEquals(SavedKind.FILE, SavedKindRules.parse("file"))
        assertEquals(SavedKind.LINK, SavedKindRules.parse("link"))
        assertEquals(SavedKind.ALL, SavedKindRules.parse(null))
        assertEquals(SavedKind.ALL, SavedKindRules.parse("nope"))
        assertEquals("photo", SavedKindRules.kv(SavedKind.PHOTO))
        assertEquals("Избранное пусто", SavedKindRules.emptyTitle(SavedKind.ALL))
        assertEquals("Нет фото", SavedKindRules.emptyTitle(SavedKind.PHOTO))
        assertEquals("В Избранном нет файлов.", SavedKindRules.emptyBody(SavedKind.FILE))
        val miss = SavedKindRules.emptyCopy(SavedKind.PHOTO, "кот")
        assertEquals("Ничего не найдено", miss.title)
        val idle = SavedKindRules.emptyCopy(SavedKind.ALL, "")
        assertEquals(SavedMessagesRules.IDLE_TITLE, idle.title)
        val photos = SavedKindRules.emptyCopy(SavedKind.PHOTO, "")
        assertEquals("Нет фото", photos.title)
        assertEquals("В Избранном нет фото и видео.", photos.body)
    }
}
