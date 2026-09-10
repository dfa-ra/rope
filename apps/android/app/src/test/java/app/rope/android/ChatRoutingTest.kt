package app.rope.android

import app.rope.android.data.ChatIds
import app.rope.android.data.ChatRouting
import app.rope.android.data.RopeGroup
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatRoutingTest {
    private val alice = "a".repeat(64)
    private val bob = "b".repeat(64)
    private val gid = "11111111-2222-3333-4444-555555555555"
    private val group = RopeGroup(gid, "команда", 1, listOf(alice, bob))

    @Test
    fun knownGroupMemberStaysInTheGroupThread() {
        assertEquals(ChatIds.group(gid), ChatRouting.mediaChatId(gid, bob, listOf(group)))
        assertEquals(ChatIds.group(gid), ChatRouting.mediaChatId(gid.uppercase(), alice.uppercase(), listOf(group)))
    }

    @Test
    fun unknownGroupFallsBackToDirectChat() {
        assertEquals(bob, ChatRouting.mediaChatId(gid, bob, emptyList()))
        assertEquals(bob, ChatRouting.mediaChatId(gid, bob, listOf(RopeGroup("other", "x", 1, listOf(bob)))))
        assertEquals(bob, ChatRouting.mediaChatId(null, bob, listOf(group)))
        assertEquals(bob, ChatRouting.mediaChatId("null", bob, listOf(group)))
    }

    @Test
    fun nonMemberDoesNotLandInTheGroupThread() {
        val outsider = "c".repeat(64)
        assertEquals(outsider, ChatRouting.mediaChatId(gid, outsider, listOf(group)))
    }
}
