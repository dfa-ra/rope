package app.rope.android

enum class NavMode { Push, SwitchTab, Reset }

enum class BackLayer {
    CloseImage,
    DismissCall,
    CancelRecording,
    CloseEmoji,
    CloseSearch,
    CloseDialog,
    ClearMessageQuery,
    ClearChatQuery,
    CancelForward,
    CancelComposer,
    CancelPendingMedia,
    Pop,
    Exit,
}

/** Local Compose overlays that live outside [UiState]. */
data class OverlayHints(
    val emojiOpen: Boolean = false,
    val searchOpen: Boolean = false,
    val dialogOpen: Boolean = false,
)

/**
 * In-app back stack for the single-Activity Compose host.
 * Hardware/gesture Back and on-screen arrows share [decide] / [pop].
 */
object BackStack {
    val tabRoots: Set<Screen> = NavRules.tabs.map { it.screen }.toSet()

    fun currentStack(backStack: List<Screen>, screen: Screen): List<Screen> = when {
        backStack.isEmpty() -> listOf(screen)
        backStack.last() != screen -> backStack + screen
        else -> backStack
    }

    fun current(stack: List<Screen>): Screen = stack.lastOrNull() ?: Screen.Start

    fun canPop(stack: List<Screen>): Boolean = stack.size > 1

    fun push(stack: List<Screen>, dest: Screen): List<Screen> {
        val cur = stack.ifEmpty { listOf(Screen.Start) }
        if (cur.last() == dest) return cur
        if (dest == Screen.Chat && cur.last() == Screen.NewGroup) {
            return cur.dropLast(1) + dest
        }
        if (dest != Screen.Chat) {
            val idx = cur.lastIndexOf(dest)
            if (idx >= 0) return cur.take(idx + 1)
        }
        return cur + dest
    }

    fun switchTab(stack: List<Screen>, dest: Screen): List<Screen> {
        if (dest == Screen.Home) return listOf(Screen.Home)
        val keepHome = stack.firstOrNull() == Screen.Home && dest != Screen.Home
        return if (keepHome) listOf(Screen.Home, dest) else listOf(dest)
    }

    fun reset(dest: Screen): List<Screen> = listOf(dest)

    fun pop(stack: List<Screen>): List<Screen> =
        if (stack.size <= 1) stack else stack.dropLast(1)

    fun listForForward(stack: List<Screen>): List<Screen> {
        val trimmed = stack.dropLastWhile {
            it == Screen.Chat || it == Screen.GroupInfo || it == Screen.NewGroup || it == Screen.PeerProfile || it == Screen.Folders
        }
        return when (trimmed.lastOrNull()) {
            Screen.Chats, Screen.Groups, Screen.People, Screen.Calls -> trimmed
            Screen.Home -> listOf(Screen.Home, Screen.Chats)
            else -> listOf(Screen.Chats)
        }
    }

    fun apply(stack: List<Screen>, dest: Screen, mode: NavMode): List<Screen> = when (mode) {
        NavMode.Push -> push(stack, dest)
        NavMode.SwitchTab -> switchTab(stack, dest)
        NavMode.Reset -> reset(dest)
    }

    fun composerBack(hasReply: Boolean, hasEdit: Boolean, pendingCount: Int): BackLayer? = when {
        hasReply || hasEdit -> BackLayer.CancelComposer
        pendingCount > 0 -> BackLayer.CancelPendingMedia
        else -> null
    }

    /** ComposerBar (and lock-to-record) only exist on Chat. Profile / group-info Back must Pop. */
    fun composerLive(screen: Screen): Boolean = screen == Screen.Chat

    fun decide(state: UiState, hints: OverlayHints = OverlayHints()): BackLayer = when {
        state.call != null -> BackLayer.DismissCall
        state.viewingImage != null -> BackLayer.CloseImage
        composerLive(state.screen) && (state.recording || state.recordingVideoNote) -> BackLayer.CancelRecording
        hints.emojiOpen -> BackLayer.CloseEmoji
        hints.searchOpen -> BackLayer.CloseSearch
        hints.dialogOpen -> BackLayer.CloseDialog
        state.messageQuery.isNotBlank() -> BackLayer.ClearMessageQuery
        state.chatQuery.isNotBlank() -> BackLayer.ClearChatQuery
        state.forwarding != null -> BackLayer.CancelForward
        else -> {
            val composer = if (composerLive(state.screen)) {
                composerBack(
                    state.replyTo != null,
                    state.editTarget != null,
                    state.pendingAttachments.size,
                )
            } else {
                null
            }
            composer ?: if (canPop(currentStack(state.backStack, state.screen))) BackLayer.Pop else BackLayer.Exit
        }
    }

    fun consumesSystemBack(state: UiState, hints: OverlayHints = OverlayHints()): Boolean =
        decide(state, hints) != BackLayer.Exit
}
