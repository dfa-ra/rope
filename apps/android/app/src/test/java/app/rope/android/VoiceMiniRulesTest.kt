package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.SavedMessagesRules
import app.rope.android.data.VoiceMiniRules
import app.rope.android.data.VoicePlayback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceMiniRulesTest {
    @Test
    fun localStoreStaysV6() {
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun hiddenInThePlayingThreadAndWhenIdle() {
        assertFalse(
            VoiceMiniRules.visible(
                activeId = "v1",
                inPlayingThread = true,
                liveCall = false,
                allowChrome = true,
            ),
        )
        assertFalse(
            VoiceMiniRules.visible(
                activeId = null,
                inPlayingThread = false,
                liveCall = false,
                allowChrome = true,
            ),
        )
        assertFalse(
            VoiceMiniRules.visible(
                activeId = "v1",
                inPlayingThread = false,
                liveCall = true,
                allowChrome = true,
            ),
        )
        assertFalse(
            VoiceMiniRules.visible(
                activeId = "v1",
                inPlayingThread = false,
                liveCall = false,
                allowChrome = false,
            ),
        )
    }

    @Test
    fun shownOnChatsListEvenWhenPaused() {
        assertTrue(
            VoiceMiniRules.visible(
                activeId = "v1",
                inPlayingThread = false,
                liveCall = false,
                allowChrome = true,
            ),
        )
        assertTrue(
            VoiceMiniRules.inPlayingThread(true, "peer-a", "peer-a"),
        )
        assertFalse(
            VoiceMiniRules.inPlayingThread(true, "peer-a", "peer-b"),
        )
        assertFalse(
            VoiceMiniRules.inPlayingThread(false, "peer-a", "peer-a"),
        )
    }

    @Test
    fun titleAndSubtitle() {
        assertEquals(SavedMessagesRules.TITLE, VoiceMiniRules.title(null, "Анна", saved = true))
        assertEquals("Команда", VoiceMiniRules.title("Команда", "Анна", saved = false))
        assertEquals("Анна", VoiceMiniRules.title(null, "Анна", saved = false))
        assertEquals("Голосовое", VoiceMiniRules.title("  ", "  ", saved = false))
        assertEquals("1.5x", VoiceMiniRules.subtitle(VoicePlayback.SPEED_1_5X, playing = true))
        assertEquals("пауза · 1x", VoiceMiniRules.subtitle(VoicePlayback.SPEED_1X, playing = false))
    }
}
