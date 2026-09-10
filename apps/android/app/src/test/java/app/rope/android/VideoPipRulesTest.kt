package app.rope.android

import app.rope.android.data.LocalStore
import app.rope.android.data.SavedMessagesRules
import app.rope.android.data.VideoPipRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoPipRulesTest {
    @Test
    fun localStoreStaysV6() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", app.rope.android.BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, app.rope.android.BuildConfig.VERSION_CODE)
    }

    @Test
    fun hiddenInThePlayingThreadAndWhenIdle() {
        assertFalse(
            VideoPipRules.visible(
                activeId = "v1",
                inPlayingThread = true,
                liveCall = false,
                signedIn = true,
                viewing = false,
            ),
        )
        assertFalse(
            VideoPipRules.visible(
                activeId = null,
                inPlayingThread = false,
                liveCall = false,
                signedIn = true,
                viewing = false,
            ),
        )
        assertFalse(
            VideoPipRules.visible(
                activeId = "v1",
                inPlayingThread = false,
                liveCall = true,
                signedIn = true,
                viewing = false,
            ),
        )
        assertFalse(
            VideoPipRules.visible(
                activeId = "v1",
                inPlayingThread = false,
                liveCall = false,
                signedIn = false,
                viewing = false,
            ),
        )
        assertFalse(
            VideoPipRules.visible(
                activeId = "v1",
                inPlayingThread = false,
                liveCall = false,
                signedIn = true,
                viewing = true,
            ),
        )
    }

    @Test
    fun shownOnChatsListEvenWhenPaused() {
        assertTrue(
            VideoPipRules.visible(
                activeId = "v1",
                inPlayingThread = false,
                liveCall = false,
                signedIn = true,
                viewing = false,
            ),
        )
        assertTrue(VideoPipRules.inPlayingThread(true, "peer-a", "peer-a"))
        assertFalse(VideoPipRules.inPlayingThread(true, "peer-a", "peer-b"))
        assertFalse(VideoPipRules.inPlayingThread(false, "peer-a", "peer-a"))
    }

    @Test
    fun titleSubtitleAndInline() {
        assertEquals(SavedMessagesRules.TITLE, VideoPipRules.title(null, "Анна", saved = true))
        assertEquals("Команда", VideoPipRules.title("Команда", "Анна", saved = false))
        assertEquals("Анна", VideoPipRules.title(null, "Анна", saved = false))
        assertEquals(VideoPipRules.FALLBACK_TITLE, VideoPipRules.title("  ", "  ", saved = false))
        assertEquals("видео", VideoPipRules.subtitle(playing = true))
        assertEquals("пауза", VideoPipRules.subtitle(playing = false))
        assertTrue(VideoPipRules.showInline("v1", "v1", screenIsChat = true, viewing = false, liveCall = false))
        assertFalse(VideoPipRules.showInline("v1", "v1", screenIsChat = false, viewing = false, liveCall = false))
        assertFalse(VideoPipRules.showInline("v1", "v1", screenIsChat = true, viewing = true, liveCall = false))
        assertFalse(VideoPipRules.showInline("v1", "v1", screenIsChat = true, viewing = false, liveCall = true))
        assertFalse(VideoPipRules.showInline("v1", "v2", screenIsChat = true, viewing = false, liveCall = false))
        assertEquals("Закрыть", VideoPipRules.CLOSE)
        assertEquals("Смотреть", VideoPipRules.PLAY)
        assertEquals("Пауза", VideoPipRules.PAUSE)
    }
}
