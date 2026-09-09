package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
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

    private fun img(
        id: String,
        ts: Long,
        kind: MessageKind = MessageKind.IMAGE,
        deleted: Boolean = false,
        localPath: String? = "/x.jpg",
    ) = ChatMessage(
        id = id,
        peerDeviceId = "peer",
        outgoing = false,
        text = "фото",
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = ts,
        kind = kind,
        localPath = localPath,
        deleted = deleted,
    )
}
