package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.SavedMessagesRules
import app.rope.android.data.SavedRoundChip
import app.rope.android.data.SavedRoundRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedRoundRulesTest {
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
        assertTrue(SavedRoundRules.shows(SavedMessagesRules.ID, isGroup = false))
        assertFalse(SavedRoundRules.shows("peer", isGroup = false))
        assertFalse(SavedRoundRules.shows(SavedMessagesRules.ID, isGroup = true))
        assertEquals(listOf(SavedRoundChip.ALL, SavedRoundChip.ROUND), SavedRoundRules.chips())
        assertEquals("Все", SavedRoundRules.label(SavedRoundChip.ALL))
        assertEquals("Кружки", SavedRoundRules.label(SavedRoundChip.ROUND))
        assertEquals("0.3.52", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(76, BuildConfig.VERSION_CODE)
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun roundChipKeepsLiveVideoNotesOnly() {
        val round = msg("1", MessageKind.VIDEO_NOTE)
        val video = msg("2", MessageKind.VIDEO)
        val photo = msg("3", MessageKind.IMAGE)
        val file = msg("4", MessageKind.FILE)
        val note = msg("5", MessageKind.TEXT)
        val voice = msg("6", MessageKind.VOICE)
        val gone = msg("7", MessageKind.VIDEO_NOTE, deleted = true)
        assertTrue(SavedRoundRules.hits(round, SavedRoundChip.ROUND))
        assertFalse(SavedRoundRules.hits(video, SavedRoundChip.ROUND))
        assertFalse(SavedRoundRules.hits(photo, SavedRoundChip.ROUND))
        assertFalse(SavedRoundRules.hits(file, SavedRoundChip.ROUND))
        assertFalse(SavedRoundRules.hits(note, SavedRoundChip.ROUND))
        assertFalse(SavedRoundRules.hits(voice, SavedRoundChip.ROUND))
        assertFalse(SavedRoundRules.hits(gone, SavedRoundChip.ROUND))
        assertTrue(SavedRoundRules.hits(photo, SavedRoundChip.ALL))
        assertTrue(SavedRoundRules.hits(gone, SavedRoundChip.ALL))
        val all = listOf(round, video, photo, file, note, voice, gone)
        assertEquals(listOf("1"), SavedRoundRules.apply(all, SavedRoundChip.ROUND).map { it.id })
        assertEquals(all.size, SavedRoundRules.apply(all, SavedRoundChip.ALL).size)
    }

    @Test
    fun kvAndEmptyCopy() {
        assertEquals(SavedRoundChip.ROUND, SavedRoundRules.parse("round"))
        assertEquals(SavedRoundChip.ALL, SavedRoundRules.parse(null))
        assertEquals(SavedRoundChip.ALL, SavedRoundRules.parse("video"))
        assertEquals(SavedRoundChip.ALL, SavedRoundRules.parse("voice"))
        assertEquals(SavedRoundChip.ALL, SavedRoundRules.parse("round\n"))
        assertEquals(SavedRoundChip.ALL, SavedRoundRules.parse("round\r"))
        assertEquals("round", SavedRoundRules.kv(SavedRoundChip.ROUND))
        assertEquals("all", SavedRoundRules.kv(SavedRoundChip.ALL))
        assertEquals(SavedMessagesRules.IDLE_TITLE, SavedRoundRules.emptyTitle(SavedRoundChip.ALL))
        assertEquals("Нет кружков", SavedRoundRules.emptyTitle(SavedRoundChip.ROUND))
        assertEquals("В Избранном нет кружков.", SavedRoundRules.emptyBody(SavedRoundChip.ROUND))
        assertFalse(SavedRoundRules.EMPTY_BODY.contains('\n'))
        val miss = SavedRoundRules.emptyCopy(SavedRoundChip.ROUND, "кот")
        assertEquals("Ничего не найдено", miss.title)
        val idle = SavedRoundRules.emptyCopy(SavedRoundChip.ALL, "")
        assertEquals(SavedMessagesRules.IDLE_TITLE, idle.title)
        val rounds = SavedRoundRules.emptyCopy(SavedRoundChip.ROUND, "")
        assertEquals("Нет кружков", rounds.title)
        assertEquals("В Избранном нет кружков.", rounds.body)
        assertEquals(6, LocalStore.VERSION)
    }
}
