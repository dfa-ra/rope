package app.rope.android

import app.rope.android.data.GroupChatUx
import app.rope.android.data.LocalStore
import app.rope.android.data.MentionAllRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MentionAllRulesTest {
    @Test
    fun inheritStoreAndToken() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("всем", MentionAllRules.TOKEN)
        assertEquals("@всем", MentionAllRules.LABEL)
        assertTrue(MentionAllRules.include(isGroup = true))
        assertFalse(MentionAllRules.include(isGroup = false))
        assertTrue(MentionAllRules.isAll("@всем"))
        assertTrue(MentionAllRules.isAll("Всем"))
        assertFalse(MentionAllRules.isAll("@Анна"))
    }

    @Test
    fun groupNamesLeadWithVsemAndDmOmitsIt() {
        val raw = listOf(" Анна ", "Боря", "всем", "")
        assertEquals(listOf("всем", "Анна", "Боря"), MentionAllRules.names(raw, isGroup = true))
        assertEquals(listOf("Анна", "Боря"), MentionAllRules.names(raw, isGroup = false))
        assertEquals(listOf("всем"), MentionAllRules.names(emptyList(), isGroup = true))
        assertTrue(MentionAllRules.names(emptyList(), isGroup = false).isEmpty())
    }

    @Test
    fun mentionSpansHighlightAtVsemInGroups() {
        val names = MentionAllRules.names(listOf("Анна", "Боря"), isGroup = true)
        val raw = "Эй, @всем и @Анна"
        val spans = GroupChatUx.mentionSpans(raw, names)
        assertEquals(2, spans.size)
        assertEquals("@всем", raw.substring(spans[0]))
        assertEquals("@Анна", raw.substring(spans[1]))
        val folded = "смотри @Всем"
        val hit = GroupChatUx.mentionSpans(folded, names)
        assertEquals(1, hit.size)
        assertEquals("@Всем", folded.substring(hit[0]))
        assertTrue(GroupChatUx.mentionSpans("@всем", MentionAllRules.names(emptyList(), isGroup = false)).isEmpty())
    }
}
