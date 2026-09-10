package app.rope.android

import app.rope.android.data.EmojiPack
import app.rope.android.data.LocalStore
import app.rope.android.data.RecentEmojiRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentEmojiRulesTest {
    @Test
    fun rememberMovesToFrontAndCaps() {
        assertEquals(emptyList<String>(), RecentEmojiRules.remember(emptyList(), "  "))
        assertEquals(listOf("😀"), RecentEmojiRules.remember(emptyList(), "😀"))
        assertEquals(listOf("👍", "😀"), RecentEmojiRules.remember(listOf("😀"), "👍"))
        assertEquals(listOf("😀", "👍"), RecentEmojiRules.remember(listOf("👍", "😀"), "😀"))
        val many = (1..RecentEmojiRules.CAP).map { "e$it" }
        val overflow = RecentEmojiRules.remember(many, "🆕")
        assertEquals(RecentEmojiRules.CAP, overflow.size)
        assertEquals("🆕", overflow.first())
        assertEquals("e23", overflow.last())
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun jsonRoundtripAndGarbage() {
        assertEquals(emptyList<String>(), RecentEmojiRules.parse(null))
        assertEquals(emptyList<String>(), RecentEmojiRules.parse(""))
        assertEquals(emptyList<String>(), RecentEmojiRules.parse("{nope}"))
        val json = RecentEmojiRules.toJson(listOf(" 😂 ", "😂", "👍"))
        assertEquals(listOf("😂", "👍"), RecentEmojiRules.parse(json))
        assertTrue(json.startsWith("["))
        assertEquals(6, LocalStore.VERSION)
    }

    @Test
    fun pickerTabsAndShown() {
        val recents = listOf("🔥")
        val tabs = RecentEmojiRules.tabs(recents)
        assertEquals(RecentEmojiRules.ID, tabs.first().id)
        assertEquals("Недавние", tabs.first().label)
        assertEquals(recents, tabs.first().emojis)
        assertEquals(EmojiPack.categories.size + 1, tabs.size)
        assertEquals(RecentEmojiRules.ID, RecentEmojiRules.initialCategory(recents))
        assertEquals(EmojiPack.categories.first().id, RecentEmojiRules.initialCategory(emptyList()))
        assertEquals(recents, RecentEmojiRules.shown(RecentEmojiRules.ID, "", recents))
        assertEquals(EmojiPack.smileys, RecentEmojiRules.shown("smileys", "", recents))
        assertTrue(RecentEmojiRules.shown("smileys", "еда", recents).contains("🍕"))
        assertEquals(6, LocalStore.VERSION)
    }
}
