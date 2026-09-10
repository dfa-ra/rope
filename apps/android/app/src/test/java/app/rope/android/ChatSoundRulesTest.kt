package app.rope.android

import app.rope.android.data.ChatPrefs
import app.rope.android.data.ChatSoundRules
import app.rope.android.data.LocalStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatSoundRulesTest {
    @Test
    fun localStoreStaysV6() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, BuildConfig.VERSION_CODE)
    }

    @Test
    fun normalizeUnknownAndBlankToDefault() {
        assertEquals(ChatSoundRules.DEFAULT, ChatSoundRules.normalize(null))
        assertEquals(ChatSoundRules.DEFAULT, ChatSoundRules.normalize("  "))
        assertEquals(ChatSoundRules.DEFAULT, ChatSoundRules.normalize("trombone"))
        assertEquals(ChatSoundRules.NOTE, ChatSoundRules.normalize("note"))
        assertEquals(ChatSoundRules.NONE, ChatSoundRules.normalize("none"))
        assertEquals(ChatSoundRules.PING, ChatSoundRules.normalize("ping"))
        assertEquals(ChatSoundRules.POP, ChatSoundRules.normalize("pop"))
    }

    @Test
    fun silentSuppressesChannelAndCustomTonesPlay() {
        assertTrue(ChatSoundRules.isSilent(ChatSoundRules.NONE))
        assertFalse(ChatSoundRules.isSilent(ChatSoundRules.DEFAULT))
        assertFalse(ChatSoundRules.playsTone(ChatSoundRules.DEFAULT))
        assertFalse(ChatSoundRules.playsTone(ChatSoundRules.NONE))
        assertTrue(ChatSoundRules.playsTone(ChatSoundRules.NOTE))
        assertTrue(ChatSoundRules.suppressChannel(ChatSoundRules.NONE))
        assertTrue(ChatSoundRules.suppressChannel(ChatSoundRules.PING))
        assertFalse(ChatSoundRules.suppressChannel(ChatSoundRules.DEFAULT))
        assertEquals(ChatSoundRules.TONE_PING, ChatSoundRules.tone(ChatSoundRules.PING))
        assertEquals("нота", ChatSoundRules.label(ChatSoundRules.NOTE))
        assertEquals("по умолчанию", ChatSoundRules.label(""))
        assertTrue(ChatSoundRules.showsPicker(saved = false))
        assertFalse(ChatSoundRules.showsPicker(saved = true))
        assertEquals(ChatSoundRules.TITLE, "Звук уведомлений")
        assertEquals(5, ChatSoundRules.OPTIONS.size)
    }

    @Test
    fun prefsRoundtripKeepsSoundWithoutSchemaBump() {
        val prefs = ChatPrefs(sound = ChatSoundRules.NOTE, muted = false)
        val got = ChatPrefs.parse(prefs.toJson())
        assertEquals(ChatSoundRules.NOTE, got.sound)
        assertFalse(got.muted)
        val missing = ChatPrefs.parse("""{"pinned":false,"muted":false}""")
        assertEquals(ChatSoundRules.DEFAULT, ChatSoundRules.normalize(missing.sound))
    }
}
