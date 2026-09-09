package app.rope.android

import app.rope.android.ui.SplashTiming
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstantUiTest {
    @Test
    fun chatHasOwnHeaderAndNoGlobalBar() {
        assertFalse(InstantUi.showsAppBar(Screen.Chat))
        assertFalse(InstantUi.showsAppBar(Screen.GroupInfo))
        assertFalse(InstantUi.showsAppBar(Screen.NewGroup))
        assertFalse(InstantUi.showsAppBar(Screen.PeerProfile))
        assertFalse(InstantUi.showsAppBar(Screen.Scheduled))
        assertTrue(InstantUi.showsAppBar(Screen.Chats))
        assertTrue(InstantUi.showsAppBar(Screen.Home))
        assertTrue(InstantUi.showsAppBar(Screen.Status))
        assertTrue(InstantUi.showsAppBar(Screen.Settings))
    }

    @Test
    fun chromeNeverShowsBackOrHomeLabel() {
        assertFalse(InstantUi.showsChromeBack())
        assertFalse(InstantUi.showsHomeLabel())
        Screen.entries.forEach { screen ->
            assertFalse(screen.name, InstantUi.showsHomeAction(screen, signedIn = true))
            assertFalse(screen.name, NavRules.showsHomeAction(screen, signedIn = true))
        }
    }

    @Test
    fun tabAndChatHopsAreInstant() {
        assertTrue(InstantUi.instantTransition(Screen.Chats, Screen.Groups))
        assertTrue(InstantUi.instantTransition(Screen.Chats, Screen.Chat))
        assertTrue(InstantUi.instantTransition(Screen.Chat, Screen.Chats))
        assertTrue(InstantUi.instantTransition(Screen.Chat, Screen.PeerProfile))
        assertTrue(InstantUi.instantTransition(Screen.PeerProfile, Screen.Chat))
        assertTrue(InstantUi.instantTransition(Screen.Chat, Screen.Scheduled))
        assertTrue(InstantUi.instantTransition(Screen.Scheduled, Screen.PeerProfile))
        assertTrue(InstantUi.instantTransition(Screen.People, Screen.Status))
        assertFalse(InstantUi.instantTransition(Screen.Home, Screen.Chats))
        assertFalse(InstantUi.instantTransition(Screen.Start, Screen.Join))
    }

    @Test
    fun busyNeverBlocksMessengerTabs() {
        assertFalse(InstantUi.busyBlocksUi(Screen.Chats))
        assertFalse(InstantUi.busyBlocksUi(Screen.Chat))
        assertFalse(InstantUi.busyBlocksUi(Screen.Status))
        assertFalse(InstantUi.busyBlocksUi(Screen.People))
        assertTrue(InstantUi.busyBlocksUi(Screen.Provision))
        assertTrue(InstantUi.busyBlocksUi(Screen.Join))
        assertFalse(
            SplashTiming.shouldShowLongLoad(
                2_000,
                busy = true,
                hasError = false,
                callActive = false,
                screen = Screen.Chats,
            ),
        )
        assertTrue(
            SplashTiming.shouldShowLongLoad(
                2_000,
                busy = true,
                hasError = false,
                callActive = false,
                screen = Screen.Join,
            ),
        )
    }
}
