package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.SavedMessagesRules
import app.rope.android.data.SavedVideoChip
import app.rope.android.data.SavedVideoRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedVideoRulesTest {
    private fun msg(
        id: String,
        kind: MessageKind,
        deleted: Boolean = false,
    ) = ChatMessage(
        id = id,
        peerDeviceId = SavedMessagesRules.ID,
        outgoing = true,
        text = "",
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        kind = kind,
        deleted = deleted,
    )

    @Test
    fun showsOnlyOnSavedThread() {
        assertTrue(SavedVideoRules.shows(SavedMessagesRules.ID, isGroup = false))
        assertFalse(SavedVideoRules.shows("peer", isGroup = false))
        assertFalse(SavedVideoRules.shows(SavedMessagesRules.ID, isGroup = true))
        assertEquals(listOf(SavedVideoChip.ALL, SavedVideoChip.VIDEO), SavedVideoRules.chips())
        assertEquals("Все", SavedVideoRules.label(SavedVideoChip.ALL))
        assertEquals("Видео", SavedVideoRules.label(SavedVideoChip.VIDEO))
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun videoChipKeepsLiveVideoOnly() {
        val video = msg("1", MessageKind.VIDEO)
        val photo = msg("2", MessageKind.IMAGE)
        val file = msg("3", MessageKind.FILE)
        val note = msg("4", MessageKind.TEXT)
        val round = msg("5", MessageKind.VIDEO_NOTE)
        val voice = msg("6", MessageKind.VOICE)
        val gone = msg("7", MessageKind.VIDEO, deleted = true)
        assertTrue(SavedVideoRules.hits(video, SavedVideoChip.VIDEO))
        assertFalse(SavedVideoRules.hits(photo, SavedVideoChip.VIDEO))
        assertFalse(SavedVideoRules.hits(file, SavedVideoChip.VIDEO))
        assertFalse(SavedVideoRules.hits(note, SavedVideoChip.VIDEO))
        assertFalse(SavedVideoRules.hits(round, SavedVideoChip.VIDEO))
        assertFalse(SavedVideoRules.hits(voice, SavedVideoChip.VIDEO))
        assertFalse(SavedVideoRules.hits(gone, SavedVideoChip.VIDEO))
        assertTrue(SavedVideoRules.hits(photo, SavedVideoChip.ALL))
        assertTrue(SavedVideoRules.hits(gone, SavedVideoChip.ALL))
        val all = listOf(video, photo, file, note, round, voice, gone)
        assertEquals(listOf("1"), SavedVideoRules.apply(all, SavedVideoChip.VIDEO).map { it.id })
        assertEquals(all.size, SavedVideoRules.apply(all, SavedVideoChip.ALL).size)
    }

    @Test
    fun kvAndEmptyCopy() {
        assertEquals(SavedVideoChip.VIDEO, SavedVideoRules.parse("video"))
        assertEquals(SavedVideoChip.ALL, SavedVideoRules.parse(null))
        assertEquals(SavedVideoChip.ALL, SavedVideoRules.parse("photo"))
        assertEquals(SavedVideoChip.ALL, SavedVideoRules.parse("voice"))
        assertEquals(SavedVideoChip.ALL, SavedVideoRules.parse("video\n"))
        assertEquals(SavedVideoChip.ALL, SavedVideoRules.parse("video\r"))
        assertEquals("video", SavedVideoRules.kv(SavedVideoChip.VIDEO))
        assertEquals("all", SavedVideoRules.kv(SavedVideoChip.ALL))
        assertEquals(SavedMessagesRules.IDLE_TITLE, SavedVideoRules.emptyTitle(SavedVideoChip.ALL))
        assertEquals("Нет видео", SavedVideoRules.emptyTitle(SavedVideoChip.VIDEO))
        assertEquals("В Избранном нет видео.", SavedVideoRules.emptyBody(SavedVideoChip.VIDEO))
        assertFalse(SavedVideoRules.EMPTY_BODY.contains('\n'))
        val miss = SavedVideoRules.emptyCopy(SavedVideoChip.VIDEO, "кот")
        assertEquals("Ничего не найдено", miss.title)
        val idle = SavedVideoRules.emptyCopy(SavedVideoChip.ALL, "")
        assertEquals(SavedMessagesRules.IDLE_TITLE, idle.title)
        val videos = SavedVideoRules.emptyCopy(SavedVideoChip.VIDEO, "")
        assertEquals("Нет видео", videos.title)
        assertEquals("В Избранном нет видео.", videos.body)
        assertEquals(6, LocalStore.VERSION)
    }
}
