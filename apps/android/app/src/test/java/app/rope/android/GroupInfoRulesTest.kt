package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.GroupInfoRules
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupInfoRulesTest {
    @Test
    fun photosAreNewestFirstSkipDeletedAndNonImages() {
        val older = img("a", 10L)
        val newer = img("b", 20L)
        val text = img("t", 30L, kind = MessageKind.TEXT)
        val deleted = img("d", 40L, deleted = true)
        val voice = img("v", 50L, kind = MessageKind.VOICE)
        val missing = img("c", 15L, localPath = null)
        val photos = GroupInfoRules.photos(listOf(older, newer, text, deleted, voice, missing))
        assertEquals(listOf("b", "c", "a"), photos.map { it.id })
        assertFalse(photos.any { it.deleted })
        assertTrue(photos.all { it.kind == MessageKind.IMAGE })
        assertEquals("Общие медиа", GroupInfoRules.sectionLabel(0))
        assertEquals("Общие медиа · 3", GroupInfoRules.sectionLabel(3))
        assertEquals(3, GroupInfoRules.GRID_COLUMNS)
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
        groupId = "g1",
    )
}
