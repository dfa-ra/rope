package app.rope.android

import app.rope.android.data.ChatMessage
import app.rope.android.data.GroupChatUx
import app.rope.android.data.LocalStore
import app.rope.android.data.MentionJumpRules
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MentionJumpRulesTest {
    @Test
    fun inheritStoreAndNames() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals(MentionJumpRules.MARK, "@")
        assertTrue(MentionJumpRules.namesForMe("Аня").contains("Аня"))
        assertTrue(MentionJumpRules.namesForMe("Аня").contains(GroupChatUx.YOU))
        assertTrue(MentionJumpRules.namesForMe("Аня").contains("всем"))
        assertFalse(MentionJumpRules.namesForMe("Аня\nбот").contains("Аня\nбот"))
        assertTrue(MentionJumpRules.namesForMe("Аня\u0000x").contains(GroupChatUx.YOU))
    }

    @Test
    fun hitsIncomingMentionNotOutgoing() {
        val mine = msg("me", outgoing = true, text = "@Аня привет")
        val hit = msg("u1", outgoing = false, text = "эй @Аня")
        val miss = msg("u2", outgoing = false, text = "без имени")
        val gone = msg("u3", outgoing = false, text = "@Аня", deleted = true)
        assertFalse(MentionJumpRules.hits(mine, "Аня"))
        assertTrue(MentionJumpRules.hits(hit, "Аня"))
        assertFalse(MentionJumpRules.hits(miss, "Аня"))
        assertFalse(MentionJumpRules.hits(gone, "Аня"))
        assertTrue(MentionJumpRules.hits(msg("u4", false, "@всем сбор"), "Боря"))
        assertTrue(MentionJumpRules.hits(msg("u5", false, "@Вы тут?"), "Боря"))
    }

    @Test
    fun unreadIdsFromAnchorAndNextWraps() {
        val read = msg("r", false, "старое @Аня", ts = 10)
        val first = msg("u1", false, "@Аня раз", ts = 20)
        val skip = msg("n", false, "нет", ts = 25)
        val second = msg("u2", false, "смотри @Аня", ts = 30)
        val msgs = listOf(read, first, skip, second)
        assertTrue(MentionJumpRules.unreadIds(msgs, "Аня", null, isGroup = true).isEmpty())
        assertTrue(MentionJumpRules.unreadIds(msgs, "Аня", "u1", isGroup = false).isEmpty())
        val ids = MentionJumpRules.unreadIds(msgs, "Аня", "u1", isGroup = true)
        assertEquals(listOf("u1", "u2"), ids)
        assertFalse(ids.contains("r"))
        assertEquals("u1", MentionJumpRules.nextId(ids, null))
        assertEquals("u2", MentionJumpRules.nextId(ids, "u1"))
        assertEquals("u1", MentionJumpRules.nextId(ids, "u2"))
        assertNull(MentionJumpRules.nextId(emptyList(), "u1"))
        assertTrue(MentionJumpRules.showFab(ids))
        assertFalse(MentionJumpRules.showFab(emptyList()))
    }

    private fun msg(
        id: String,
        outgoing: Boolean,
        text: String,
        deleted: Boolean = false,
        ts: Long = 1L,
    ) = ChatMessage(
        id = id,
        peerDeviceId = "g:crew",
        outgoing = outgoing,
        text = text,
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = ts,
        kind = MessageKind.GROUP_TEXT,
        deleted = deleted,
    )
}
