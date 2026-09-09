package app.rope.android

import app.rope.android.data.ChatIds
import app.rope.android.data.ChatRouting
import app.rope.android.data.GroupTextRules
import app.rope.android.data.RopeGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GroupTextRulesTest {
    private val alice = "a".repeat(64)
    private val bob = "b".repeat(64)
    private val gid = "11111111-2222-3333-4444-555555555555"
    private val group = RopeGroup(gid, "команда", 1, listOf(alice, bob))

    @Test
    fun knownGroupAndMemberInsertsIntoGroupThread() {
        assertEquals(ChatIds.group(gid), GroupTextRules.chatId(gid, bob, listOf(group)))
        assertEquals(ChatIds.group(gid), GroupTextRules.chatId(gid.uppercase(), alice.uppercase(), listOf(group)))
    }

    @Test
    fun unknownGroupIsDroppedNotDumpedIntoDirectChat() {
        assertNull(GroupTextRules.chatId(gid, bob, emptyList()))
        assertNull(GroupTextRules.chatId(gid, bob, listOf(RopeGroup("other", "x", 1, listOf(bob)))))
        assertEquals(bob, ChatRouting.mediaChatId(gid, bob, emptySet()))
        assertNull(GroupTextRules.chatId(gid, bob, emptyList()))
    }

    @Test
    fun nonMemberIsDroppedEvenWhenTheGroupIsKnown() {
        val outsider = "c".repeat(64)
        assertNull(GroupTextRules.chatId(gid, outsider, listOf(group)))
        assertEquals(ChatIds.group(gid), ChatRouting.mediaChatId(gid, outsider, setOf(gid)))
        assertNull(GroupTextRules.chatId(gid, outsider, listOf(group)))
    }

    @Test
    fun blankOrNullGroupIdIsDropped() {
        assertNull(GroupTextRules.chatId(null, bob, listOf(group)))
        assertNull(GroupTextRules.chatId("", bob, listOf(group)))
        assertNull(GroupTextRules.chatId("null", bob, listOf(group)))
        assertNull(GroupTextRules.chatId(gid, "", listOf(group)))
        assertNull(GroupTextRules.chatId(gid, null, listOf(group)))
    }
}
