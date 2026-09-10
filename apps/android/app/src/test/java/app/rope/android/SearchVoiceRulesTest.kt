package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageSearch
import app.rope.android.data.MessageStatus
import app.rope.android.data.SearchVoiceRules
import app.rope.android.data.ThreadEmptyRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchVoiceRulesTest {
    private fun msg(
        kind: MessageKind,
        text: String = "",
        deleted: Boolean = false,
    ) = ChatMessage(
        "m",
        "p",
        false,
        text,
        MessageStatus.DELIVERED_TO_DEVICE,
        1L,
        kind = kind,
        deleted = deleted,
    )

    @Test
    fun chipKeepsOnlyLiveVoice() {
        val voice = msg(MessageKind.VOICE)
        val photo = msg(MessageKind.IMAGE)
        val gone = msg(MessageKind.VOICE, deleted = true)
        assertTrue(SearchVoiceRules.hits(voice, voiceOnly = true))
        assertFalse(SearchVoiceRules.hits(photo, voiceOnly = true))
        assertFalse(SearchVoiceRules.hits(gone, voiceOnly = true))
        assertTrue(SearchVoiceRules.hits(photo, voiceOnly = false))
        assertTrue(SearchVoiceRules.matches(voice, "", voiceOnly = true))
        assertTrue(MessageSearch.matches(voice, ""))
        assertFalse(SearchVoiceRules.matches(photo, "", voiceOnly = true))
        assertEquals("Голосовые", SearchVoiceRules.LABEL)
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun searchingAndEmptyCopy() {
        assertTrue(SearchVoiceRules.searching("", voiceOnly = true))
        assertFalse(SearchVoiceRules.searching("", voiceOnly = false))
        assertTrue(SearchVoiceRules.searching("привет", voiceOnly = false))
        assertEquals(SearchVoiceRules.EMPTY_BODY, SearchVoiceRules.emptyBody("", voiceOnly = true))
        assertEquals(ThreadEmptyRules.searchBody("анн"), SearchVoiceRules.emptyBody("анн", voiceOnly = true))
        val copy = ThreadEmptyRules.copy("", saved = false, voiceOnly = true)
        assertEquals(ThreadEmptyRules.SEARCH_TITLE, copy.title)
        assertEquals(SearchVoiceRules.EMPTY_BODY, copy.body)
        val chat = UiState(screen = Screen.Chat, backStack = listOf(Screen.Chats, Screen.Chat))
        assertEquals(BackLayer.ClearMessageQuery, BackStack.decide(chat.copy(searchVoice = true)))
        assertEquals(BackLayer.Pop, BackStack.decide(chat))
        assertEquals(6, LocalStore.VERSION)
    }
}
