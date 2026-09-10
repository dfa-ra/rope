package app.rope.android

import app.rope.android.data.BlockRules
import app.rope.android.data.ChatIds
import app.rope.android.data.DirectoryDevice
import app.rope.android.data.LocalStore
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockRulesTest {
    private val anna = DirectoryDevice("PEER-1", "", "Анна", ByteArray(0), "")

    @Test
    fun canBlockPeersNotSelfSavedOrGroup() {
        assertTrue(BlockRules.canBlock("peer-1", "me"))
        assertTrue(BlockRules.canBlock("  PEER-1  ", "me"))
        assertFalse(BlockRules.canBlock(null, "me"))
        assertFalse(BlockRules.canBlock("", "me"))
        assertFalse(BlockRules.canBlock("me", "me"))
        assertFalse(BlockRules.canBlock("ME", "me"))
        assertFalse(BlockRules.canBlock(SavedMessagesRules.ID, "me"))
        assertFalse(BlockRules.canBlock(ChatIds.group("g-1"), "me"))
        assertTrue(BlockRules.canBlock("peer-1", null))
        assertEquals("Заблокировать", BlockRules.ACTION_BLOCK)
        assertEquals("Разблокировать", BlockRules.ACTION_UNBLOCK)
        assertEquals("blocked", BlockRules.KEY)
    }

    @Test
    fun applyAndIsBlockedAreCaseInsensitive() {
        val blocked = BlockRules.apply(emptySet(), "PEER-1", true)
        assertTrue(BlockRules.isBlocked(blocked, "peer-1"))
        assertTrue(BlockRules.isBlocked(blocked, "PEER-1"))
        assertFalse(BlockRules.isBlocked(blocked, "peer-2"))
        assertEquals(emptySet<String>(), BlockRules.apply(blocked, "peer-1", false))
        assertEquals(blocked, BlockRules.apply(blocked, SavedMessagesRules.ID, true))
        assertEquals(blocked, BlockRules.apply(blocked, "", true))
    }

    @Test
    fun dropDirectOnlyOnOneToOne() {
        val ids = setOf("peer-1")
        assertTrue(BlockRules.dropDirect(ids, "peer-1", groupChat = false))
        assertFalse(BlockRules.dropDirect(ids, "peer-1", groupChat = true))
        assertFalse(BlockRules.dropDirect(ids, "peer-2", groupChat = false))
        assertEquals("Заблокировать", BlockRules.action(false))
        assertEquals("Разблокировать", BlockRules.action(true))
    }

    @Test
    fun encodeParseAndLabel() {
        val raw = BlockRules.encode(setOf("b", "a"))
        assertEquals(setOf("a", "b"), BlockRules.parse(raw))
        assertEquals(emptySet<String>(), BlockRules.parse(null))
        assertEquals(emptySet<String>(), BlockRules.parse("not-json"))
        assertEquals("Анна", BlockRules.label("peer-1", listOf(anna)))
        assertEquals("abcd1234…", BlockRules.preview("abcdefghijklmnop"))
        assertEquals("short", BlockRules.preview("short"))
    }

    @Test
    fun noSchemaBump() {
        assertEquals(6, LocalStore.VERSION)
    }
}
