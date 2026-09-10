package app.rope.android

import app.rope.android.data.ChatHomeRules
import app.rope.android.data.ChatIds
import app.rope.android.data.LocalStore
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatHomeRulesTest {
    @Test
    fun inheritStoreAndRejectControlChars() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("На главный экран", ChatHomeRules.LABEL)
        assertFalse(ChatHomeRules.LABEL.contains("FCM", ignoreCase = true))
        val dm = "aabbccddeeff00112233445566778899aabbccddeeff00112233445566778899"
        assertEquals(dm, ChatHomeRules.sanitizeId("  $dm  "))
        assertTrue(ChatHomeRules.canPin(dm))
        assertTrue(ChatHomeRules.canPin(SavedMessagesRules.ID))
        assertTrue(ChatHomeRules.canPin(ChatIds.group("g-uuid")))
        assertNull(ChatHomeRules.sanitizeId("id\nbad"))
        assertNull(ChatHomeRules.sanitizeId("id\rbad"))
        assertNull(ChatHomeRules.sanitizeId("id\u0000bad"))
        assertNull(ChatHomeRules.sanitizeId(""))
        assertNull(ChatHomeRules.sanitizeId(" "))
        assertNull(ChatHomeRules.sanitizeId("a".repeat(ChatHomeRules.MAX_ID + 1)))
        assertNull(ChatHomeRules.sanitizeId("id with space"))
        assertNull(ChatHomeRules.sanitizeId("javascript:alert(1)"))
        assertFalse(ChatHomeRules.canPin(null))
    }

    @Test
    fun hideWhileForwarding() {
        assertTrue(ChatHomeRules.show(forwarding = false))
        assertFalse(ChatHomeRules.show(forwarding = true))
    }

    @Test
    fun consumeOnlyOpenChatAction() {
        val id = "dev-1"
        assertEquals(id, ChatHomeRules.consume(ChatHomeRules.ACTION, id))
        assertNull(ChatHomeRules.consume("android.intent.action.MAIN", id))
        assertNull(ChatHomeRules.consume(ChatHomeRules.ACTION, "id\nbad"))
        assertNull(ChatHomeRules.consume(null, id))
        assertNull(ChatHomeRules.consume(ChatHomeRules.ACTION, null))
    }

    @Test
    fun shortcutIdsAreStableAndDistinct() {
        val a = "aaa111"
        val b = ChatIds.group("bbbb")
        assertEquals("rope-chat:$a", ChatHomeRules.shortcutId(a))
        assertNotEquals(ChatHomeRules.shortcutId(a), ChatHomeRules.shortcutId(b))
        assertEquals(ChatHomeRules.shortcutId(a), ChatHomeRules.shortcutId(a))
        assertTrue(ChatHomeRules.matches(a, a))
        assertTrue(ChatHomeRules.matches(a.uppercase(), a))
        assertFalse(ChatHomeRules.matches(SavedMessagesRules.ID, a))
        assertFalse(ChatHomeRules.matches(b, a))
    }

    @Test
    fun labelsTrimAndFallBack() {
        assertEquals("Анна", ChatHomeRules.shortLabel("Анна"))
        assertEquals("Чат", ChatHomeRules.shortLabel("  "))
        assertEquals("Длинное имя…", ChatHomeRules.shortLabel("Длинное имя чата"))
        assertEquals("Команда", ChatHomeRules.longLabel("Команда"))
        assertEquals("Чат", ChatHomeRules.longLabel(""))
        val long = "Это очень длинное название беседы для ярлыка"
        assertTrue(ChatHomeRules.longLabel(long).endsWith("…"))
        assertTrue(ChatHomeRules.longLabel(long).length <= 30)
    }
}
