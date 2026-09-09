package app.rope.android

import app.rope.android.data.UnreadBadgeKind
import app.rope.android.data.UnreadBadgeRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnreadBadgeRulesTest {
    @Test
    fun mutedUnreadIsGrayNotAccent() {
        assertEquals(UnreadBadgeKind.NONE, UnreadBadgeRules.kind(0, muted = false))
        assertEquals(UnreadBadgeKind.NONE, UnreadBadgeRules.kind(0, muted = true))
        assertEquals(UnreadBadgeKind.NONE, UnreadBadgeRules.kind(-1, muted = false))
        assertEquals(UnreadBadgeKind.ACCENT, UnreadBadgeRules.kind(1, muted = false))
        assertEquals(UnreadBadgeKind.MUTED, UnreadBadgeRules.kind(1, muted = true))
        assertEquals(UnreadBadgeKind.MUTED, UnreadBadgeRules.kind(12, muted = true))
        assertEquals(UnreadBadgeKind.ACCENT, UnreadBadgeRules.kind(12, muted = false))
    }

    @Test
    fun labelCapsAt99Plus() {
        assertEquals("", UnreadBadgeRules.label(0))
        assertEquals("", UnreadBadgeRules.label(-4))
        assertEquals("1", UnreadBadgeRules.label(1))
        assertEquals("99", UnreadBadgeRules.label(99))
        assertEquals("99+", UnreadBadgeRules.label(100))
        assertEquals("99+", UnreadBadgeRules.label(1_000))
        assertFalse(UnreadBadgeRules.visible(0))
        assertTrue(UnreadBadgeRules.visible(1))
        assertTrue(UnreadBadgeRules.visible(100))
    }

    @Test
    fun unreadTitleIsEmphasizedEvenWhenMuted() {
        assertFalse(UnreadBadgeRules.emphasizeTitle(0))
        assertTrue(UnreadBadgeRules.emphasizeTitle(1))
        assertTrue(UnreadBadgeRules.emphasizeTitle(100))
        assertEquals(UnreadBadgeKind.MUTED, UnreadBadgeRules.kind(3, muted = true))
        assertTrue(UnreadBadgeRules.emphasizeTitle(3))
    }
}
