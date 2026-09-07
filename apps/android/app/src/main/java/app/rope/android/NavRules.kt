package app.rope.android

import app.rope.android.data.ChatListMode
import app.rope.android.data.Conversation
import app.rope.android.data.DirectoryDevice
import app.rope.android.data.MessageKind

object BrandLinks {
    const val GITHUB = "https://github.com/dfa-ra/rope"
    const val RELEASES = "https://github.com/dfa-ra/rope/releases"
}

data class MainTab(
    val screen: Screen,
    val label: String,
)

object NavRules {
    val tabs: List<MainTab> = listOf(
        MainTab(Screen.Home, "Главная"),
        MainTab(Screen.Chats, "Чаты"),
        MainTab(Screen.Groups, "Группы"),
        MainTab(Screen.Calls, "Звонки"),
        MainTab(Screen.Status, "Статус"),
    )

    fun showsBottomBar(screen: Screen, signedIn: Boolean): Boolean {
        if (!signedIn) return false
        return screen == Screen.Home ||
            screen == Screen.Chats ||
            screen == Screen.Groups ||
            screen == Screen.Calls ||
            screen == Screen.Status ||
            screen == Screen.People ||
            screen == Screen.Settings
    }

    fun canOpenHome(screen: Screen, signedIn: Boolean): Boolean =
        signedIn && screen != Screen.Home &&
            screen != Screen.Start &&
            screen != Screen.Provision &&
            screen != Screen.Join

    fun titleOpensHome(screen: Screen, signedIn: Boolean): Boolean = canOpenHome(screen, signedIn)

    fun selectedTab(screen: Screen): Screen? = when (screen) {
        Screen.Home, Screen.Invite, Screen.People, Screen.Settings -> Screen.Home
        Screen.Chats, Screen.Chat -> Screen.Chats
        Screen.Groups, Screen.NewGroup, Screen.GroupInfo -> Screen.Groups
        Screen.Calls -> Screen.Calls
        Screen.Status -> Screen.Status
        else -> null
    }

    fun listMode(screen: Screen): ChatListMode = when (screen) {
        Screen.Groups -> ChatListMode.GROUPS
        Screen.Calls -> ChatListMode.CALLS
        else -> ChatListMode.ALL
    }

    fun groupsOf(conversations: List<Conversation>): List<Conversation> =
        conversations.filter { it.isGroup }

    fun callsOf(conversations: List<Conversation>): List<Conversation> =
        conversations.filter { it.last?.kind == MessageKind.CALL }

    fun peopleOf(devices: List<DirectoryDevice>, selfId: String?): List<DirectoryDevice> =
        devices.filter { it.deviceId != selfId }

    fun refreshesLists(screen: Screen): Boolean =
        screen == Screen.Chats ||
            screen == Screen.Groups ||
            screen == Screen.Calls ||
            screen == Screen.Home ||
            screen == Screen.People
}
