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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavRulesTest {
    @Test
    fun homeIsReachableFromEverySignedInDestination() {
        val signedIn = listOf(
            Screen.Chats, Screen.Chat, Screen.Groups, Screen.Calls, Screen.Status,
            Screen.People, Screen.Invite, Screen.Settings, Screen.NewGroup, Screen.GroupInfo, Screen.PeerProfile,
        )
        signedIn.forEach { screen ->
            assertTrue(screen.name, NavRules.canOpenHome(screen, signedIn = true))
            assertTrue(screen.name, NavRules.titleOpensHome(screen, signedIn = true))
            assertFalse(screen.name, NavRules.showsHomeAction(screen, signedIn = true))
        }
        assertFalse(NavRules.canOpenHome(Screen.Home, signedIn = true))
        assertFalse(NavRules.canOpenHome(Screen.Chats, signedIn = false))
        assertFalse(NavRules.canOpenHome(Screen.Start, signedIn = false))
        assertFalse(NavRules.canOpenHome(Screen.Join, signedIn = true))
    }

    @Test
    fun homeHasNoTabBarMessengerHasTabBar() {
        assertFalse(NavRules.showsBottomBar(Screen.Home, true))
        assertFalse(NavRules.isMessengerTab(Screen.Home))
        assertFalse(NavRules.isMessengerShell(Screen.Home))
        assertEquals(Screen.Home, NavRules.signedInRoot)
        assertEquals(Screen.Chats, NavRules.messengerRoot)
        assertTrue(NavRules.showsBottomBar(Screen.Chats, true))
        assertTrue(NavRules.showsBottomBar(Screen.Groups, true))
        assertTrue(NavRules.showsBottomBar(Screen.Calls, true))
        assertTrue(NavRules.showsBottomBar(Screen.People, true))
        assertTrue(NavRules.showsBottomBar(Screen.Status, true))
        assertFalse(NavRules.showsBottomBar(Screen.Chat, true))
        assertFalse(NavRules.showsBottomBar(Screen.Settings, true))
        assertFalse(NavRules.showsBottomBar(Screen.Invite, true))
        assertFalse(NavRules.showsBottomBar(Screen.Home, false))
        assertFalse(NavRules.showsBottomBar(Screen.Start, false))
        assertTrue(NavRules.isMessengerShell(Screen.Chat))
        assertTrue(NavRules.isMessengerShell(Screen.People))
        assertTrue(NavRules.isMessengerShell(Screen.PeerProfile))
        assertFalse(NavRules.showsBottomBar(Screen.PeerProfile, true))
    }

    @Test
    fun selectedTabMapsNestedScreens() {
        assertNull(NavRules.selectedTab(Screen.Home))
        assertNull(NavRules.selectedTab(Screen.Invite))
        assertNull(NavRules.selectedTab(Screen.Settings))
        assertEquals(Screen.People, NavRules.selectedTab(Screen.People))
        assertEquals(Screen.Chats, NavRules.selectedTab(Screen.Chat))
        assertEquals(Screen.Chats, NavRules.selectedTab(Screen.PeerProfile))
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
    fun homeShortcutsStayInApp() {
        assertEquals(HomeCtas.messenger, HomeCtas.shortcuts[0])
        assertEquals(HomeCtas.status, HomeCtas.shortcuts[1])
        assertEquals(HomeCtas.settings, HomeCtas.shortcuts[2])
        assertEquals(
            listOf(Screen.Chats, Screen.Status, Screen.Settings),
            HomeCtas.shortcuts.map { it.destination },
        )
        assertEquals("Перейти к мессенджеру", HomeCtas.messenger.label)
        assertEquals("К статусу", HomeCtas.status.label)
        assertEquals("Настройки", HomeCtas.settings.label)
        HomeCtas.shortcuts.forEach { shortcut ->
            val label = shortcut.label.lowercase()
            assertFalse(shortcut.label, label.contains("лендинг"))
            assertFalse(shortcut.label, label.contains("landing"))
            assertFalse(shortcut.label, label.contains("скачать"))
            assertFalse(shortcut.label, label.contains("download"))
            assertFalse(shortcut.label, label.contains("releases"))
            assertFalse(shortcut.label, label.contains("о приложении"))
            assertFalse(shortcut.label, label.contains("about"))
            assertFalse(shortcut.label, label.contains("github"))
        }
    }

    @Test
    fun guestStillHasNoAdminQr() {
        for (role in listOf("guest", "member", "GUEST", null, "")) {
            assertFalse(role.toString(), NavRules.showsInviteCta(role))
            assertFalse(role.toString(), NavRules.homePeopleHint(role, emptyList()).contains("QR", ignoreCase = true))
        }
        assertTrue(NavRules.showsInviteCta("owner"))
        assertTrue(NavRules.showsInviteCta("OWNER"))
        assertTrue(NavRules.homePeopleHint("owner", emptyList()).contains("QR"))
    }

    @Test
    fun chromeBarDropsWordmarkAndUsername() {
        assertFalse(NavRules.chromeShowsWordmark())
        assertFalse(NavRules.chromeShowsUsername())
        Screen.entries.forEach { screen ->
            assertFalse(screen.name, NavRules.showsHomeAction(screen, signedIn = true))
            assertFalse(screen.name, NavRules.showsHomeAction(screen, signedIn = false))
        }
        assertTrue(NavRules.chromeShowsUserChip(true))
        assertFalse(NavRules.chromeShowsUserChip(false))
        assertTrue(NavRules.contentShowsOwnName(Screen.Home))
        assertTrue(NavRules.contentShowsOwnName(Screen.People))
        assertTrue(NavRules.contentShowsOwnName(Screen.Settings))
        assertFalse(NavRules.contentShowsOwnName(Screen.Chats))
        assertFalse(NavRules.contentShowsOwnName(Screen.Calls))
        assertFalse(NavRules.contentShowsOwnName(Screen.Status))
        assertEquals("", NavRules.chromeTitle(Screen.Home))
        assertEquals("Чаты", NavRules.chromeTitle(Screen.Chats))
        assertEquals("Группы", NavRules.chromeTitle(Screen.Groups))
        assertEquals("Звонки", NavRules.chromeTitle(Screen.Calls))
        assertEquals("Люди", NavRules.chromeTitle(Screen.People))
        assertEquals("Статус", NavRules.chromeTitle(Screen.Status))
        assertEquals("Настройки", NavRules.chromeTitle(Screen.Settings))
        assertEquals("Профиль", NavRules.chromeTitle(Screen.PeerProfile))
        assertEquals("офлайн", NavRules.chromeTitle(Screen.Home, offline = true))
        Screen.entries.forEach { screen ->
            val title = NavRules.chromeTitle(screen)
            assertFalse(screen.name, title.contains("Rope"))
            assertFalse(screen.name, title.contains("·"))
            val offline = NavRules.chromeTitle(screen, offline = true)
            assertEquals("офлайн", offline)
            assertFalse(screen.name, offline.contains("Rope"))
        }
    }

    @Test
    fun messengerTabsExcludeHomeAndRefreshLists() {
        assertEquals(5, NavRules.tabs.size)
        assertEquals(
            listOf(Screen.Chats, Screen.Groups, Screen.Calls, Screen.People, Screen.Status),
            NavRules.tabs.map { it.screen },
        )
        assertFalse(NavRules.tabs.any { it.screen == Screen.Home })
        assertTrue(NavRules.refreshesLists(Screen.Home))
        assertTrue(NavRules.refreshesLists(Screen.People))
        assertFalse(NavRules.refreshesLists(Screen.Chat))
    }
}
