package app.rope.android

import app.rope.android.data.CallInfo
import app.rope.android.data.CallLinkState
import app.rope.android.data.CallPhase
import app.rope.android.data.ChatMessage
import app.rope.android.data.MessageKind
import app.rope.android.data.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackStackTest {
    private val photo = ChatMessage(
        id = "img",
        peerDeviceId = "p",
        outgoing = false,
        text = "фото",
        status = MessageStatus.DELIVERED_TO_DEVICE,
        timestampMs = 1L,
        kind = MessageKind.IMAGE,
    )

    @Test
    fun pushChatThenBackReturnsToList() {
        var stack = listOf(Screen.Chats)
        stack = BackStack.push(stack, Screen.Chat)
        assertEquals(listOf(Screen.Chats, Screen.Chat), stack)
        assertEquals(Screen.Chat, BackStack.current(stack))
        assertTrue(BackStack.canPop(stack))
        stack = BackStack.pop(stack)
        assertEquals(listOf(Screen.Chats), stack)
        assertFalse(BackStack.canPop(stack))
        assertEquals(BackLayer.Exit, BackStack.decide(UiState(screen = Screen.Chats, backStack = stack)))
    }

    @Test
    fun pushPhotoThenChatTakesTwoBacks() {
        var stack = BackStack.push(listOf(Screen.Chats), Screen.Chat)
        val withPhoto = UiState(screen = Screen.Chat, backStack = stack, viewingImage = photo)
        assertEquals(BackLayer.CloseImage, BackStack.decide(withPhoto))
        val chatOnly = withPhoto.copy(viewingImage = null)
        assertEquals(BackLayer.Pop, BackStack.decide(chatOnly))
        stack = BackStack.pop(stack)
        assertEquals(listOf(Screen.Chats), stack)
        assertEquals(BackLayer.Exit, BackStack.decide(UiState(screen = Screen.Chats, backStack = stack)))
    }

    @Test
    fun rootBackIsEmptyExit() {
        val start = listOf(Screen.Start)
        val home = listOf(Screen.Home)
        val chats = listOf(Screen.Chats)
        assertFalse(BackStack.canPop(start))
        assertFalse(BackStack.canPop(home))
        assertFalse(BackStack.canPop(chats))
        assertEquals(start, BackStack.pop(start))
        assertEquals(BackLayer.Exit, BackStack.decide(UiState(screen = Screen.Home, backStack = home)))
        assertEquals(BackLayer.Exit, BackStack.decide(UiState(screen = Screen.Chats, backStack = chats)))
        assertFalse(BackStack.consumesSystemBack(UiState(screen = Screen.Chats, backStack = chats)))
    }

    @Test
    fun groupChatPopsToGroups() {
        var stack = BackStack.push(listOf(Screen.Groups), Screen.Chat)
        assertEquals(listOf(Screen.Groups, Screen.Chat), stack)
        stack = BackStack.pop(stack)
        assertEquals(listOf(Screen.Groups), stack)
        assertEquals(Screen.Groups, BackStack.current(stack))
    }

    @Test
    fun peopleChatPopsToPeople() {
        val stack = BackStack.pop(BackStack.push(listOf(Screen.Home, Screen.People), Screen.Chat))
        assertEquals(listOf(Screen.Home, Screen.People), stack)
    }

    @Test
    fun settingsPopsBackToHome() {
        val stack = BackStack.push(listOf(Screen.Home), Screen.Settings)
        assertEquals(listOf(Screen.Home, Screen.Settings), stack)
        assertEquals(
            BackLayer.Pop,
            BackStack.decide(UiState(screen = Screen.Settings, backStack = stack)),
        )
        assertEquals(listOf(Screen.Home), BackStack.pop(stack))
    }

    @Test
    fun homeToChatsThenBackThenExit() {
        var stack = BackStack.push(listOf(Screen.Home), Screen.Chats)
        assertEquals(listOf(Screen.Home, Screen.Chats), stack)
        assertEquals(BackLayer.Pop, BackStack.decide(UiState(screen = Screen.Chats, backStack = stack)))
        stack = BackStack.pop(stack)
        assertEquals(listOf(Screen.Home), stack)
        assertEquals(BackLayer.Exit, BackStack.decide(UiState(screen = Screen.Home, backStack = stack)))
    }

    @Test
    fun nestedPhotoChatListHomeThenExit() {
        var stack = BackStack.push(listOf(Screen.Home), Screen.Chats)
        stack = BackStack.push(stack, Screen.Chat)
        assertEquals(listOf(Screen.Home, Screen.Chats, Screen.Chat), stack)
        var state = UiState(screen = Screen.Chat, backStack = stack, viewingImage = photo)
        assertEquals(BackLayer.CloseImage, BackStack.decide(state))
        state = state.copy(viewingImage = null)
        assertEquals(BackLayer.Pop, BackStack.decide(state))
        stack = BackStack.pop(stack)
        state = state.copy(screen = stack.last(), backStack = stack)
        assertEquals(listOf(Screen.Home, Screen.Chats), stack)
        assertEquals(BackLayer.Pop, BackStack.decide(state))
        stack = BackStack.pop(stack)
        state = state.copy(screen = stack.last(), backStack = stack)
        assertEquals(BackLayer.Exit, BackStack.decide(state))
    }

    @Test
    fun tabRootsAreMessengerOnly() {
        assertEquals(
            setOf(Screen.Chats, Screen.Groups, Screen.Calls, Screen.People, Screen.Status),
            BackStack.tabRoots,
        )
        assertFalse(Screen.Home in BackStack.tabRoots)
        var stack = BackStack.push(listOf(Screen.Home), Screen.Chats)
        stack = BackStack.switchTab(stack, Screen.People)
        assertEquals(listOf(Screen.Home, Screen.People), stack)
        stack = BackStack.pop(stack)
        assertEquals(listOf(Screen.Home), stack)
        assertEquals(BackLayer.Exit, BackStack.decide(UiState(screen = Screen.Home, backStack = stack)))
    }

    @Test
    fun switchTabDropsNestedChatAndKeepsHomePrefix() {
        assertEquals(
            listOf(Screen.Groups),
            BackStack.switchTab(listOf(Screen.Chats, Screen.Chat), Screen.Groups),
        )
        assertEquals(
            listOf(Screen.Home, Screen.Groups),
            BackStack.switchTab(listOf(Screen.Home, Screen.Chats, Screen.Chat), Screen.Groups),
        )
        assertEquals(listOf(Screen.Home), BackStack.switchTab(listOf(Screen.Home, Screen.Chats), Screen.Home))
        assertEquals(listOf(Screen.Status), BackStack.switchTab(listOf(Screen.Chats), Screen.Status))
    }

    @Test
    fun creatingGroupReplacesNewGroupWithChat() {
        val stack = BackStack.push(listOf(Screen.Groups, Screen.NewGroup), Screen.Chat)
        assertEquals(listOf(Screen.Groups, Screen.Chat), stack)
    }

    @Test
    fun reopenSameScreenPopsToExisting() {
        val stack = BackStack.push(listOf(Screen.Home, Screen.Chats, Screen.Invite), Screen.Home)
        assertEquals(listOf(Screen.Home), stack)
    }

    @Test
    fun layersCloseBeforePop() {
        val stack = listOf(Screen.Chats, Screen.Chat)
        val base = UiState(screen = Screen.Chat, backStack = stack)
        assertEquals(BackLayer.DismissCall, BackStack.decide(base.copy(call = ringingCall())))
        assertEquals(
            BackLayer.DismissCall,
            BackStack.decide(base.copy(call = ringingCall(), viewingImage = photo)),
        )
        assertEquals(BackLayer.CancelRecording, BackStack.decide(base.copy(recording = true)))
        assertEquals(BackLayer.CloseEmoji, BackStack.decide(base, OverlayHints(emojiOpen = true)))
        assertEquals(BackLayer.CloseSearch, BackStack.decide(base, OverlayHints(searchOpen = true)))
        assertEquals(BackLayer.CloseDialog, BackStack.decide(base, OverlayHints(dialogOpen = true)))
        assertEquals(BackLayer.ClearMessageQuery, BackStack.decide(base.copy(messageQuery = "привет")))
        assertEquals(
            BackLayer.ClearChatQuery,
            BackStack.decide(UiState(screen = Screen.Chats, backStack = listOf(Screen.Chats), chatQuery = "анна")),
        )
        assertEquals(
            BackLayer.Pop,
            BackStack.decide(
                UiState(
                    screen = Screen.Chat,
                    backStack = listOf(Screen.Chats, Screen.Archive, Screen.Chat),
                    chatQuery = "анна",
                ),
            ),
        )
        assertEquals(
            BackLayer.CancelForward,
            BackStack.decide(base.copy(screen = Screen.Chats, backStack = listOf(Screen.Chats), forwarding = photo)),
        )
        assertEquals(BackLayer.CancelComposer, BackStack.decide(base.copy(replyTo = photo)))
        assertEquals(BackLayer.CancelComposer, BackStack.composerBack(hasReply = true, hasEdit = false, pendingCount = 2))
        assertEquals(BackLayer.CancelPendingMedia, BackStack.composerBack(hasReply = false, hasEdit = false, pendingCount = 1))
        assertEquals(null, BackStack.composerBack(hasReply = false, hasEdit = false, pendingCount = 0))
        assertEquals(BackLayer.Pop, BackStack.decide(base))
    }

    @Test
    fun applyModes() {
        assertEquals(
            listOf(Screen.Chats, Screen.Chat),
            BackStack.apply(listOf(Screen.Chats), Screen.Chat, NavMode.Push),
        )
        assertEquals(
            listOf(Screen.Groups),
            BackStack.apply(listOf(Screen.Chats, Screen.Chat), Screen.Groups, NavMode.SwitchTab),
        )
        assertEquals(listOf(Screen.Chats), BackStack.apply(listOf(Screen.Start), Screen.Chats, NavMode.Reset))
    }

    @Test
    fun listForForwardPopsChat() {
        assertEquals(listOf(Screen.Chats), BackStack.listForForward(listOf(Screen.Chats, Screen.Chat)))
        assertEquals(listOf(Screen.Groups), BackStack.listForForward(listOf(Screen.Groups, Screen.Chat, Screen.GroupInfo)))
        assertEquals(listOf(Screen.Chats), BackStack.listForForward(listOf(Screen.Chats, Screen.Chat, Screen.PeerProfile)))
        assertEquals(listOf(Screen.Chats), BackStack.listForForward(listOf(Screen.Chats, Screen.Archive)))
        assertEquals(listOf(Screen.Home, Screen.Chats), BackStack.listForForward(listOf(Screen.Home)))
        assertEquals(listOf(Screen.Chats), BackStack.listForForward(emptyList()))
    }

    @Test
    fun peerProfilePopsToChat() {
        var stack = BackStack.push(listOf(Screen.Chats), Screen.Chat)
        stack = BackStack.push(stack, Screen.PeerProfile)
        assertEquals(listOf(Screen.Chats, Screen.Chat, Screen.PeerProfile), stack)
        stack = BackStack.pop(stack)
        assertEquals(listOf(Screen.Chats, Screen.Chat), stack)
        assertEquals(Screen.Chat, BackStack.current(stack))
        assertTrue(BackStack.composerLive(Screen.Chat))
        assertFalse(BackStack.composerLive(Screen.PeerProfile))
        assertFalse(BackStack.composerLive(Screen.GroupInfo))
    }

    @Test
    fun peerProfileAndGroupInfoBackKeepStagedComposer() {
        val profile = listOf(Screen.Chats, Screen.Chat, Screen.PeerProfile)
        val group = listOf(Screen.Groups, Screen.Chat, Screen.GroupInfo)
        assertEquals(
            BackLayer.Pop,
            BackStack.decide(
                UiState(screen = Screen.PeerProfile, backStack = profile, replyTo = photo),
            ),
        )
        assertEquals(
            BackLayer.Pop,
            BackStack.decide(
                UiState(screen = Screen.PeerProfile, backStack = profile, recording = true),
            ),
        )
        assertEquals(
            BackLayer.Pop,
            BackStack.decide(
                UiState(screen = Screen.GroupInfo, backStack = group, replyTo = photo),
            ),
        )
        assertEquals(
            BackLayer.Pop,
            BackStack.decide(
                UiState(screen = Screen.GroupInfo, backStack = group, recording = true),
            ),
        )
        assertEquals(
            BackLayer.CancelComposer,
            BackStack.decide(
                UiState(
                    screen = Screen.Chat,
                    backStack = listOf(Screen.Chats, Screen.Chat),
                    replyTo = photo,
                ),
            ),
        )
        assertEquals(
            BackLayer.CancelRecording,
            BackStack.decide(
                UiState(
                    screen = Screen.Chat,
                    backStack = listOf(Screen.Chats, Screen.Chat),
                    recording = true,
                ),
            ),
        )
    }

    private fun ringingCall() = CallInfo(
        callId = "c1",
        peerDeviceId = "p",
        peerName = "Анна",
        outgoing = true,
        phase = CallPhase.RINGING_OUT,
        media = "",
        link = CallLinkState.RINGING,
    )
}
