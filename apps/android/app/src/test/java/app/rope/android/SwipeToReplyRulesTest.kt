package app.rope.android

import app.rope.android.data.ChatActions
import app.rope.android.data.ChatMessage
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import app.rope.android.data.SwipeToReplyRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeToReplyRulesTest {
    @Test
    fun tapAndLongPressStaySettledSoClickableWins() {
        assertTrue(SwipeToReplyRules.stillSettled(0f, 0f))
        assertTrue(SwipeToReplyRules.stillSettled(4f, -3f))
        assertTrue(SwipeToReplyRules.stillSettled(SwipeToReplyRules.SLOP_DP - 0.1f, 0f))
        assertFalse(SwipeToReplyRules.shouldLock(0f, 0f))
        assertFalse(SwipeToReplyRules.shouldAbort(0f, 0f))
        assertFalse(SwipeToReplyRules.shouldLock(8f, 0f))
        assertFalse(SwipeToReplyRules.shouldAbort(-8f, 0f))
    }

    @Test
    fun leftSwipeLocksAndVerticalScrollAborts() {
        assertTrue(SwipeToReplyRules.shouldLock(-SwipeToReplyRules.SLOP_DP, 0f))
        assertTrue(SwipeToReplyRules.shouldLock(-40f, 10f))
        assertFalse(SwipeToReplyRules.shouldLock(-10f, -20f))
        assertTrue(SwipeToReplyRules.shouldAbort(0f, 20f))
        assertTrue(SwipeToReplyRules.shouldAbort(20f, 0f))
        assertTrue(SwipeToReplyRules.shouldAbort(-10f, 24f))
        assertFalse(SwipeToReplyRules.shouldAbort(-40f, 10f))
    }

    @Test
    fun offsetClampsLeftAndIgnoresRight() {
        assertEquals(0f, SwipeToReplyRules.offset(12f), 0f)
        assertEquals(-20f, SwipeToReplyRules.offset(-20f), 0f)
        assertEquals(-SwipeToReplyRules.MAX_DP, SwipeToReplyRules.offset(-200f), 0f)
    }

    @Test
    fun commitAtThresholdAndProgress() {
        assertFalse(SwipeToReplyRules.shouldCommit(-SwipeToReplyRules.COMMIT_DP + 1f))
        assertTrue(SwipeToReplyRules.shouldCommit(-SwipeToReplyRules.COMMIT_DP))
        assertTrue(SwipeToReplyRules.shouldCommit(-SwipeToReplyRules.MAX_DP))
        assertTrue(SwipeToReplyRules.crossedCommit(-20f, -SwipeToReplyRules.COMMIT_DP))
        assertFalse(SwipeToReplyRules.crossedCommit(-SwipeToReplyRules.COMMIT_DP, -60f))
        assertEquals(0f, SwipeToReplyRules.progress(0f), 0f)
        assertEquals(0.5f, SwipeToReplyRules.progress(-SwipeToReplyRules.COMMIT_DP / 2f), 0.001f)
        assertEquals(1f, SwipeToReplyRules.progress(-SwipeToReplyRules.COMMIT_DP), 0f)
        assertEquals(1f, SwipeToReplyRules.progress(-SwipeToReplyRules.MAX_DP), 0f)
        assertEquals(1f, SwipeToReplyRules.iconAlpha(1f), 0f)
        assertEquals(1.12f, SwipeToReplyRules.iconScale(1f), 0f)
        assertTrue(SwipeToReplyRules.iconScale(0f) < SwipeToReplyRules.iconScale(0.5f))
    }

    @Test
    fun deletedAndSelectingCannotSwipe() {
        val live = msg("a")
        val gone = msg("b", deleted = true)
        assertTrue(ChatActions.canReply(live))
        assertTrue(SwipeToReplyRules.canSwipe(live, selecting = false))
        assertFalse(SwipeToReplyRules.canSwipe(live, selecting = true))
        assertFalse(SwipeToReplyRules.canSwipe(gone, selecting = false))
        assertFalse(ChatActions.canReply(gone))
    }

    @Test
    fun albumRepliesToFirstLiveMember() {
        val gone = msg("gone", deleted = true)
        val live = msg("live")
        val later = msg("later")
        assertTrue(SwipeToReplyRules.canSwipeAlbum(listOf(gone, live, later), selecting = false))
        assertFalse(SwipeToReplyRules.canSwipeAlbum(listOf(gone, live), selecting = true))
        assertFalse(SwipeToReplyRules.canSwipeAlbum(listOf(gone), selecting = false))
        assertEquals("live", SwipeToReplyRules.replyTarget(listOf(gone, live, later))?.id)
        assertNull(SwipeToReplyRules.replyTarget(listOf(gone)))
    }

    private fun msg(id: String, deleted: Boolean = false): ChatMessage = ChatMessage(
        id = id,
        peerDeviceId = "peer",
        outgoing = false,
        text = "hi",
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        senderId = "them",
        kind = MessageKind.TEXT,
        deleted = deleted,
    )
}
