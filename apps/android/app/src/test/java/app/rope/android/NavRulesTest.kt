package app.rope.android

import app.rope.android.data.ChatListMode
import app.rope.android.data.ChatListRules
import app.rope.android.data.ChatMessage
import app.rope.android.data.Conversation
import app.rope.android.data.DirectoryDevice
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavRulesTest {
    @Test
    fun homeIsReachableFromEverySignedInDestination() {
        val signedIn = listOf(
            Screen.Chats, Screen.Chat, Screen.Groups, Screen.Calls, Screen.Status,
            Screen.People, Screen.Invite, Screen.Settings, Screen.NewGroup, Screen.GroupInfo,
        )
        signedIn.forEach { screen ->
            assertTrue(screen.name, NavRules.canOpenHome(screen, signedIn = true))
            assertTrue(screen.name, NavRules.titleOpensHome(screen, signedIn = true))
        }
        assertFalse(NavRules.canOpenHome(Screen.Home, signedIn = true))
        assertFalse(NavRules.canOpenHome(Screen.Chats, signedIn = false))
        assertFalse(NavRules.canOpenHome(Screen.Start, signedIn = false))
        assertFalse(NavRules.canOpenHome(Screen.Join, signedIn = true))
    }

    @Test
    fun bottomBarShowsOnMainTabsAndPeople() {
        assertTrue(NavRules.showsBottomBar(Screen.Home, true))
        assertTrue(NavRules.showsBottomBar(Screen.Chats, true))
        assertTrue(NavRules.showsBottomBar(Screen.Groups, true))
        assertTrue(NavRules.showsBottomBar(Screen.Calls, true))
        assertTrue(NavRules.showsBottomBar(Screen.Status, true))
        assertTrue(NavRules.showsBottomBar(Screen.People, true))
        assertFalse(NavRules.showsBottomBar(Screen.Chat, true))
        assertFalse(NavRules.showsBottomBar(Screen.Home, false))
        assertFalse(NavRules.showsBottomBar(Screen.Start, false))
    }

    @Test
    fun selectedTabMapsNestedScreens() {
        assertEquals(Screen.Home, NavRules.selectedTab(Screen.People))
        assertEquals(Screen.Chats, NavRules.selectedTab(Screen.Chat))
        assertEquals(Screen.Groups, NavRules.selectedTab(Screen.NewGroup))
        assertEquals(Screen.Calls, NavRules.selectedTab(Screen.Calls))
        assertEquals(Screen.Status, NavRules.selectedTab(Screen.Status))
        assertEquals(null, NavRules.selectedTab(Screen.Start))
    }

    @Test
    fun listModeAndFilters() {
        assertEquals(ChatListMode.GROUPS, NavRules.listMode(Screen.Groups))
        assertEquals(ChatListMode.CALLS, NavRules.listMode(Screen.Calls))
        assertEquals(ChatListMode.ALL, NavRules.listMode(Screen.Chats))
        val call = ChatMessage("m", "p", true, "звонок", MessageStatus.DELIVERED_TO_DEVICE, 1L, kind = MessageKind.CALL)
        val group = Conversation("g:1", "Команда", "привет", true, false, null)
        val dm = Conversation("dev", "Анна", "ок", false, true, call)
        val chats = listOf(group, dm)
        assertEquals(1, NavRules.groupsOf(chats).size)
        assertEquals(1, NavRules.callsOf(chats).size)
        assertTrue(ChatListRules.visible(group, ChatListMode.GROUPS))
        assertFalse(ChatListRules.visible(dm, ChatListMode.GROUPS))
        assertTrue(ChatListRules.visible(dm, ChatListMode.CALLS))
        assertFalse(ChatListRules.visible(group, ChatListMode.CALLS))
        assertTrue(ChatListRules.matches(group, "ком", ChatListMode.GROUPS))
        assertFalse(ChatListRules.matches(group, "ком", ChatListMode.CALLS))
    }

    @Test
    fun peopleExcludeSelf() {
        val a = DirectoryDevice("a", "m1", "Аня", ByteArray(0), "", true)
        val b = DirectoryDevice("b", "m2", "Боб", ByteArray(0), "", false)
        assertEquals(listOf(b), NavRules.peopleOf(listOf(a, b), "a"))
        assertEquals(2, NavRules.peopleOf(listOf(a, b), null).size)
    }

    @Test
    fun fiveMainTabsAndListRefresh() {
        assertEquals(5, NavRules.tabs.size)
        assertEquals(listOf(Screen.Home, Screen.Chats, Screen.Groups, Screen.Calls, Screen.Status), NavRules.tabs.map { it.screen })
        assertTrue(NavRules.refreshesLists(Screen.Home))
        assertTrue(NavRules.refreshesLists(Screen.People))
        assertFalse(NavRules.refreshesLists(Screen.Chat))
    }
}
