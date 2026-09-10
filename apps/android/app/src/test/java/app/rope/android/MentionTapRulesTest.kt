package app.rope.android

import app.rope.android.data.ChatIds
import app.rope.android.data.DirectoryDevice
import app.rope.android.data.GroupChatUx
import app.rope.android.data.LocalStore
import app.rope.android.data.MentionTapRules
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MentionTapRulesTest {
    @Test
    fun canOpenOtherMembersNotSelf() {
        assertTrue(MentionTapRules.canOpen(true, "peer-1", "me"))
        assertTrue(MentionTapRules.canOpen(true, "  PEER-1  ", "me"))
        assertTrue(MentionTapRules.canOpen(true, "peer-1", null))
        assertFalse(MentionTapRules.canOpen(false, "peer-1", "me"))
        assertFalse(MentionTapRules.canOpen(true, null, "me"))
        assertFalse(MentionTapRules.canOpen(true, "", "me"))
        assertFalse(MentionTapRules.canOpen(true, "me", "me"))
        assertFalse(MentionTapRules.canOpen(true, "ME", "me"))
        assertFalse(MentionTapRules.canOpen(true, SavedMessagesRules.ID, "me"))
        assertFalse(MentionTapRules.canOpen(true, ChatIds.group("g-1"), "me"))
        assertEquals("Написать", MentionTapRules.ACTION)
        assertEquals("Нет в справочнике", MentionTapRules.NOTICE_MISSING)
    }

    @Test
    fun tapResolvesDirectoryNameAndSkipsSelf() {
        val anna = person("a", "Анна")
        val bob = person("b", "Боб")
        val text = "привет @Анна и @Боб"
        val annaAt = text.indexOf("@Анна")
        val bobAt = text.indexOf("@Боб")
        assertEquals("a", MentionTapRules.deviceIdAt(text, annaAt + 1, listOf(anna, bob), "Я", "me"))
        assertEquals("b", MentionTapRules.deviceIdAt(text, bobAt, listOf(anna, bob), "Я", "me"))
        assertNull(MentionTapRules.deviceIdAt(text, 0, listOf(anna, bob), "Я", "me"))
        val selfText = "ок @Вы"
        val youAt = selfText.indexOf("@Вы")
        assertNull(MentionTapRules.deviceIdAt(selfText, youAt, listOf(anna), "Я", "me"))
        assertNull(MentionTapRules.deviceIdAt("ок @Я", 3, listOf(anna), "Я", "me"))
        assertEquals("Анна", MentionTapRules.nameInSpan(text, annaAt until (annaAt + "@Анна".length)))
        assertEquals(6, LocalStore.VERSION)
        assertEquals(GroupChatUx.YOU, "Вы")
    }

    private fun person(id: String, name: String): DirectoryDevice =
        DirectoryDevice(id, "m$id", name, ByteArray(0), "", online = false)
}
