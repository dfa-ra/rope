package app.rope.android

import app.rope.android.data.ChatIds
import app.rope.android.data.LocalStore
import app.rope.android.data.VoiceDraft
import app.rope.android.data.VoiceDraftRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceDraftRulesTest {
    @Test
    fun inheritAndKeep() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.52", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(76, BuildConfig.VERSION_CODE)
        assertTrue(VoiceDraftRules.keep(locked = true, recording = true))
        assertFalse(VoiceDraftRules.keep(locked = false, recording = true))
        assertFalse(VoiceDraftRules.keep(locked = true, recording = false))
        assertTrue(VoiceDraftRules.cancelOnLeave(locked = false))
        assertFalse(VoiceDraftRules.cancelOnLeave(locked = true))
    }

    @Test
    fun parseAndRestoreSameChat() {
        assertEquals("peer-1", VoiceDraftRules.parseChatId("  peer-1  "))
        assertEquals(ChatIds.group("g9"), VoiceDraftRules.parseChatId(ChatIds.group("g9")))
        assertNull(VoiceDraftRules.parseChatId(null))
        assertNull(VoiceDraftRules.parseChatId("ab\ncd"))
        assertNull(VoiceDraftRules.parseChatId("ab\rcd"))
        assertNull(VoiceDraftRules.parseChatId("ab\u0000cd"))
        assertNull(VoiceDraftRules.parsePath("/tmp/v.m4a\n"))
        assertEquals("/data/voice.m4a", VoiceDraftRules.parsePath("/data/voice.m4a"))
        val draft = VoiceDraft(chatId = "peer-1", path = "/data/voice.m4a", durationMs = 1200)
        assertNotNull(VoiceDraftRules.restore(draft, "peer-1"))
        assertNull(VoiceDraftRules.restore(draft, "peer-2"))
        assertNull(VoiceDraftRules.restore(draft.copy(path = "/x\n"), "peer-1"))
        assertNull(VoiceDraftRules.restore(null, "peer-1"))
        val chat = UiState(
            screen = Screen.Chat,
            backStack = listOf(Screen.Chats, Screen.Chat),
            recording = true,
            recordingLocked = true,
        )
        assertEquals(BackLayer.Pop, BackStack.decide(chat))
        assertEquals(
            BackLayer.CancelRecording,
            BackStack.decide(chat.copy(recordingLocked = false)),
        )
        assertEquals(6, LocalStore.VERSION)
    }
}
