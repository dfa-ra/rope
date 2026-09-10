package app.rope.android

import app.rope.android.data.EmojiReplaceRules
import app.rope.android.data.LocalStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiReplaceRulesTest {
    @Test
    fun sendReplacesEmoticons() {
        assertTrue(EmojiReplaceRules.enabledFromKv(null))
        assertFalse(EmojiReplaceRules.enabledFromKv("0"))
        assertEquals("привет 🙂", EmojiReplaceRules.apply("привет :)", true))
        assertEquals(":-)", EmojiReplaceRules.apply(":-)", false))
        assertEquals("🙂", EmojiReplaceRules.apply(":-)", true))
        assertEquals("❤", EmojiReplaceRules.apply("<3", true))
        assertEquals("https://x", EmojiReplaceRules.apply("https://x", true))
        assertEquals("Заменять эмодзи", EmojiReplaceRules.TITLE)
        assertTrue(EmojiReplaceRules.hint().contains("стикер"))
        assertEquals(6, LocalStore.VERSION)
    }
}
