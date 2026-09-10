package app.rope.android

import app.rope.android.data.ChatIds
import app.rope.android.data.LocalStore
import app.rope.android.data.MuteSwipeRules
import app.rope.android.data.SavedMessagesRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MuteSwipeRulesTest {
    @Test
    fun savedAndHeaderCannotSwipe() {
        assertEquals(6, LocalStore.VERSION)
        assertFalse(MuteSwipeRules.canSwipe(SavedMessagesRules.ID, header = false))
        assertFalse(MuteSwipeRules.canSwipe("aaa111", header = true))
        assertFalse(MuteSwipeRules.canSwipe(null, header = false))
        assertFalse(MuteSwipeRules.canSwipe("", header = false))
        assertTrue(MuteSwipeRules.canSwipe("aaa111", header = false))
        assertTrue(MuteSwipeRules.canSwipe(ChatIds.group("g-uuid"), header = false))
    }

    @Test
    fun rightSwipeLocksAndCommits() {
        assertTrue(MuteSwipeRules.stillSettled(0f, 0f))
        assertTrue(MuteSwipeRules.shouldLock(MuteSwipeRules.SLOP_DP, 0f))
        assertFalse(MuteSwipeRules.shouldLock(-MuteSwipeRules.SLOP_DP, 0f))
        assertTrue(MuteSwipeRules.shouldAbort(-MuteSwipeRules.SLOP_DP - 1f, 0f))
        assertTrue(MuteSwipeRules.shouldAbort(0f, 20f))
        assertTrue(MuteSwipeRules.shouldCommit(MuteSwipeRules.COMMIT_DP))
        assertFalse(MuteSwipeRules.shouldCommit(MuteSwipeRules.COMMIT_DP - 1f))
        assertEquals(MuteSwipeRules.MAX_DP, MuteSwipeRules.offset(200f))
        assertEquals(0f, MuteSwipeRules.offset(-10f))
        assertTrue(MuteSwipeRules.crossedCommit(40f, MuteSwipeRules.COMMIT_DP))
        assertFalse(MuteSwipeRules.crossedCommit(MuteSwipeRules.COMMIT_DP, 60f))
        assertEquals(1f, MuteSwipeRules.progress(MuteSwipeRules.COMMIT_DP), 0.0001f)
        assertEquals(1.12f, MuteSwipeRules.iconScale(1f), 0.0001f)
        assertTrue(MuteSwipeRules.iconAlpha(0.5f) in 0f..1f)
    }
}
