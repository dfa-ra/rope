package app.rope.android

import app.rope.android.data.HashtagRules
import app.rope.android.data.LocalStore
import app.rope.android.data.MessageSearch
import app.rope.android.data.ChatMessage
import app.rope.android.data.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HashtagRulesTest {
    @Test
    fun findsCyrillicAndSkipsUrlFragments() {
        val text = "см. #Релиз и https://example.com/a#frag плюс #rope_ui"
        val tags = HashtagRules.spans(text).map { it.tag }
        assertEquals(listOf("#Релиз", "#rope_ui"), tags)
        assertEquals("#Релиз", HashtagRules.query("#Релиз"))
        assertEquals("#rope_ui", HashtagRules.query("rope_ui"))
    }

    @Test
    fun queryRejectsCrLfHttpAndJunk() {
        assertNull(HashtagRules.query(null))
        assertNull(HashtagRules.query(""))
        assertNull(HashtagRules.query("#"))
        assertNull(HashtagRules.query("#tag\n#evil"))
        assertNull(HashtagRules.query("#tag\r"))
        assertNull(HashtagRules.query("https://example.com"))
        assertNull(HashtagRules.query("#" + "a".repeat(HashtagRules.TAG_MAX + 1)))
        assertEquals("#a", HashtagRules.query("#a"))
        val longOk = "#" + "a".repeat(HashtagRules.TAG_MAX)
        assertEquals(longOk, HashtagRules.query(longOk))
        val msg = ChatMessage("1", "p", false, "тема #Релиз сегодня", MessageStatus.DELIVERED_TO_DEVICE, 1L)
        assertTrue(MessageSearch.matches(msg, HashtagRules.query("#Релиз")!!))
        assertFalse(MessageSearch.matches(msg, "#другое"))
        assertEquals(6, LocalStore.VERSION)
    }
}
