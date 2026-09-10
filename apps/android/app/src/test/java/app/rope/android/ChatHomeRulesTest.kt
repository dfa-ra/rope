package app.rope.android

import app.rope.android.data.LocalStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatHomeRulesTest {
    @Test
    fun inheritStoreAndBackCopy() {
        assertEquals(6, LocalStore.VERSION)
        assertEquals("0.3.51", BuildConfig.VERSION_NAME.substringBefore("-"))
        assertEquals(75, BuildConfig.VERSION_CODE)
        assertEquals("Назад", ChatHomeRules.BACK)
        assertEquals(Screen.Chats, ChatHomeRules.HOME)
    }

    @Test
    fun backShowsUnlessSelectingAndLoneChatStaysInMessenger() {
        assertTrue(ChatHomeRules.showsBack(selecting = false))
        assertFalse(ChatHomeRules.showsBack(selecting = true))
        assertTrue(ChatHomeRules.consumesExit(onChat = true))
        assertFalse(ChatHomeRules.consumesExit(onChat = false))
    }

    @Test
    fun threadWithoutListGetsChatsUnderIt() {
        assertEquals(
            listOf(Screen.Chats, Screen.Chat),
            ChatHomeRules.stackUnderThread(listOf(Screen.Chat)),
        )
        assertEquals(
            listOf(Screen.Home, Screen.Chats, Screen.Chat),
            ChatHomeRules.stackUnderThread(listOf(Screen.Home, Screen.Chat)),
        )
        assertEquals(
            listOf(Screen.Home, Screen.People, Screen.Chat),
            ChatHomeRules.stackUnderThread(listOf(Screen.Home, Screen.People, Screen.Chat)),
        )
        assertFalse(ChatHomeRules.isList(Screen.Home))
        assertTrue(ChatHomeRules.isList(Screen.Archive))
        assertEquals(
            BackLayer.ToChats,
            BackStack.decide(UiState(screen = Screen.Chat, backStack = listOf(Screen.Chat))),
        )
        assertEquals(
            BackLayer.Pop,
            BackStack.decide(UiState(screen = Screen.Chat, backStack = listOf(Screen.Chats, Screen.Chat))),
        )
    }
}
