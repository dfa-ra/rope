package app.rope.android

/**
 * Chat header «Назад» returns to the messenger list. A thread always has
 * Чаты (or the list you came from) under it so Back does not exit.
 * LocalStore stays v6.
 */
object ChatHomeRules {
    const val BACK = "Назад"
    val HOME: Screen = Screen.Chats

    private val lists: Set<Screen> = setOf(
        Screen.Chats,
        Screen.Groups,
        Screen.People,
        Screen.Archive,
        Screen.Calls,
    )

    fun showsBack(selecting: Boolean): Boolean = !selecting

    fun consumesExit(onChat: Boolean): Boolean = onChat

    fun isList(screen: Screen): Boolean = screen in lists

    fun stackUnderThread(stack: List<Screen>): List<Screen> {
        val withChat = if (stack.lastOrNull() == Screen.Chat) stack else stack + Screen.Chat
        if (withChat.any { isList(it) }) return withChat
        val keepHome = withChat.firstOrNull() == Screen.Home
        return if (keepHome) listOf(Screen.Home, HOME, Screen.Chat) else listOf(HOME, Screen.Chat)
    }
}
