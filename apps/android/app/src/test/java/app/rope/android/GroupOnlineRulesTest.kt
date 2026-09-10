package app.rope.android

import app.rope.android.data.GroupOnlineRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupOnlineRulesTest {
    @Test
    fun countsLiveMembersAndIgnoresDuplicates() {
        val members = listOf("AAA", "bbb", "ccc", "BBB")
        val online = setOf("bbb", "zzz")
        assertEquals(1, GroupOnlineRules.onlineCount(members, online))
        assertEquals(2, GroupOnlineRules.onlineCount(members, setOf("AAA", "ccc")))
        assertEquals(0, GroupOnlineRules.onlineCount(members, emptySet()))
        assertEquals(0, GroupOnlineRules.onlineCount(emptyList(), setOf("aaa")))
    }

    @Test
    fun countsSelfWhenThisDeviceIsOnline() {
        val members = listOf("me", "peer")
        assertEquals(1, GroupOnlineRules.onlineCount(members, emptySet(), myId = "me", selfOnline = true))
        assertEquals(1, GroupOnlineRules.onlineCount(members, setOf("me"), myId = "me", selfOnline = false))
        assertEquals(2, GroupOnlineRules.onlineCount(members, setOf("peer"), myId = "me", selfOnline = true))
        assertEquals(0, GroupOnlineRules.onlineCount(members, emptySet(), myId = "me", selfOnline = false))
        assertEquals(0, GroupOnlineRules.onlineCount(listOf("peer"), emptySet(), myId = "me", selfOnline = true))
    }

    @Test
    fun subtitleIsTelegramLike() {
        assertEquals("5 участников · все офлайн", GroupOnlineRules.subtitle(5, 0))
        assertEquals("5 участников · 1 в сети", GroupOnlineRules.subtitle(5, 1))
        assertEquals("3 участников · 2 в сети", GroupOnlineRules.subtitle(3, 2))
        assertTrue(GroupOnlineRules.subtitle(4, 0).contains(GroupOnlineRules.OFFLINE))
        assertFalse(GroupOnlineRules.subtitle(4, 3).contains("кто-то"))
    }
}
