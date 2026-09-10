package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.PackedLinkPreview
import app.rope.android.data.PeerProfileRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PeerProfileRulesTest {
    @Test
    fun oneToOneHeaderOpensProfileGroupOpensGroupInfo() {
        assertTrue(PeerProfileRules.headerClickable(isGroup = false, hasPeer = true))
        assertTrue(PeerProfileRules.opensPeerProfile(isGroup = false, hasPeer = true))
        assertTrue(PeerProfileRules.headerClickable(isGroup = true, hasPeer = false))
        assertFalse(PeerProfileRules.opensPeerProfile(isGroup = true, hasPeer = false))
        assertFalse(PeerProfileRules.headerClickable(isGroup = false, hasPeer = false))
        assertFalse(PeerProfileRules.opensPeerProfile(isGroup = false, hasPeer = false))
    }

    @Test
    fun photosAreNewestFirstSkipDeletedAndNonImages() {
        val older = img("a", 10L, localPath = "/a.jpg")
        val newer = img("b", 20L, localPath = "/b.jpg")
        val text = img("t", 30L, kind = MessageKind.TEXT)
        val deleted = img("d", 40L, deleted = true)
        val voice = img("v", 50L, kind = MessageKind.VOICE)
        val missingFile = img("c", 15L, localPath = null)
        val photos = PeerProfileRules.photos(listOf(older, newer, text, deleted, voice, missingFile))
        assertEquals(listOf("b", "c", "a"), photos.map { it.id })
        assertFalse(photos.any { it.deleted })
        assertTrue(photos.all { it.kind == MessageKind.IMAGE })
    }

    @Test
    fun emptyCopyAndSectionLabel() {
        assertEquals("Нет общих медиа", PeerProfileRules.emptyTitle())
        assertEquals("Фото из этой переписки появятся здесь.", PeerProfileRules.emptyBody())
        assertEquals("Общие медиа", PeerProfileRules.sectionLabel(0))
        assertEquals("Общие медиа · 3", PeerProfileRules.sectionLabel(3))
        assertEquals("Анна", PeerProfileRules.title("Анна"))
        assertEquals("Профиль", PeerProfileRules.title("  "))
        assertEquals("Профиль", PeerProfileRules.title(null))
        assertEquals(3, PeerProfileRules.GRID_COLUMNS)
    }

    @Test
    fun linksAreNewestFirstFromTextAndPackedSkipDeleted() {
        val older = img("a", 10L, kind = MessageKind.TEXT, text = "смотри https://example.com/old")
        val newer = img("b", 20L, kind = MessageKind.TEXT, text = "https://News.YCombinator.com/item?id=1 и https://example.com/b")
        val http = img("h", 30L, kind = MessageKind.TEXT, text = "http://skip.example")
        val deleted = img("d", 40L, kind = MessageKind.TEXT, text = "https://gone.example", deleted = true)
        val packed = img(
            "p",
            15L,
            kind = MessageKind.TEXT,
            text = "карточка",
            preview = PackedLinkPreview(url = "https://packed.example/x", host = "packed.example", title = "t"),
        )
        val photo = img("i", 50L)
        val links = PeerProfileRules.links(listOf(older, newer, http, deleted, packed, photo))
        assertEquals(
            listOf(
                "https://news.ycombinator.com/item?id=1",
                "https://example.com/b",
                "https://packed.example/x",
                "https://example.com/old",
            ),
            links.map { it.url },
        )
        assertEquals(listOf("b", "b", "p", "a"), links.map { it.messageId })
        assertEquals("Ссылки", PeerProfileRules.linksSectionLabel(0))
        assertEquals("Ссылки · 2", PeerProfileRules.linksSectionLabel(2))
        assertTrue(PeerProfileRules.showPhotoEmpty(0, 0))
        assertFalse(PeerProfileRules.showPhotoEmpty(0, 1))
        assertFalse(PeerProfileRules.showPhotoEmpty(1, 0))
    }

    private fun img(
        id: String,
        ts: Long,
        kind: MessageKind = MessageKind.IMAGE,
        deleted: Boolean = false,
        localPath: String? = "/x.jpg",
        text: String = "фото",
        preview: PackedLinkPreview? = null,
    ) = ChatMessage(
        id = id,
        peerDeviceId = "peer",
        outgoing = false,
        text = text,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = ts,
        kind = kind,
        localPath = localPath,
        deleted = deleted,
        linkPreview = preview,
    )
}
