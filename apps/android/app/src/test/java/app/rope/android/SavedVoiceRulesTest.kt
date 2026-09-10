package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.SavedMessagesRules
import app.rope.android.data.SavedVoiceChip
import app.rope.android.data.SavedVoiceRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedVoiceRulesTest {
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
        assertTrue(SavedVoiceRules.shows(SavedMessagesRules.ID, isGroup = false))
        assertFalse(SavedVoiceRules.shows("peer", isGroup = false))
        assertFalse(SavedVoiceRules.shows(SavedMessagesRules.ID, isGroup = true))
        assertEquals(listOf(SavedVoiceChip.ALL, SavedVoiceChip.VOICE), SavedVoiceRules.chips())
        assertEquals("Все", SavedVoiceRules.label(SavedVoiceChip.ALL))
        assertEquals("Голосовые", SavedVoiceRules.label(SavedVoiceChip.VOICE))
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun voiceChipKeepsLiveVoiceOnly() {
        val voice = msg("1", MessageKind.VOICE)
        val photo = msg("2", MessageKind.IMAGE)
        val file = msg("3", MessageKind.FILE)
        val note = msg("4", MessageKind.TEXT)
        val round = msg("5", MessageKind.VIDEO_NOTE)
        val gone = msg("6", MessageKind.VOICE, deleted = true)
        assertTrue(SavedVoiceRules.hits(voice, SavedVoiceChip.VOICE))
        assertFalse(SavedVoiceRules.hits(photo, SavedVoiceChip.VOICE))
        assertFalse(SavedVoiceRules.hits(file, SavedVoiceChip.VOICE))
        assertFalse(SavedVoiceRules.hits(note, SavedVoiceChip.VOICE))
        assertFalse(SavedVoiceRules.hits(round, SavedVoiceChip.VOICE))
        assertFalse(SavedVoiceRules.hits(gone, SavedVoiceChip.VOICE))
        assertTrue(SavedVoiceRules.hits(photo, SavedVoiceChip.ALL))
        assertTrue(SavedVoiceRules.hits(gone, SavedVoiceChip.ALL))
        val all = listOf(voice, photo, file, note, round, gone)
        assertEquals(listOf("1"), SavedVoiceRules.apply(all, SavedVoiceChip.VOICE).map { it.id })
        assertEquals(all.size, SavedVoiceRules.apply(all, SavedVoiceChip.ALL).size)
    }

    @Test
    fun kvAndEmptyCopy() {
        assertEquals(SavedVoiceChip.VOICE, SavedVoiceRules.parse("voice"))
        assertEquals(SavedVoiceChip.ALL, SavedVoiceRules.parse(null))
        assertEquals(SavedVoiceChip.ALL, SavedVoiceRules.parse("photo"))
        assertEquals("voice", SavedVoiceRules.kv(SavedVoiceChip.VOICE))
        assertEquals("all", SavedVoiceRules.kv(SavedVoiceChip.ALL))
        assertEquals(SavedMessagesRules.IDLE_TITLE, SavedVoiceRules.emptyTitle(SavedVoiceChip.ALL))
        assertEquals("Нет голосовых", SavedVoiceRules.emptyTitle(SavedVoiceChip.VOICE))
        assertEquals("В Избранном нет голосовых.", SavedVoiceRules.emptyBody(SavedVoiceChip.VOICE))
        val miss = SavedVoiceRules.emptyCopy(SavedVoiceChip.VOICE, "кот")
        assertEquals("Ничего не найдено", miss.title)
        val idle = SavedVoiceRules.emptyCopy(SavedVoiceChip.ALL, "")
        assertEquals(SavedMessagesRules.IDLE_TITLE, idle.title)
        val voices = SavedVoiceRules.emptyCopy(SavedVoiceChip.VOICE, "")
        assertEquals("Нет голосовых", voices.title)
        assertEquals("В Избранном нет голосовых.", voices.body)
        assertEquals(6, LocalStore.VERSION)
    }
}
