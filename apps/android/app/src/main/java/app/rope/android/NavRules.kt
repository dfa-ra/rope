package app.rope.android

import app.rope.android.data.ChatListMode
import app.rope.android.data.Conversation
import app.rope.android.data.DirectoryDevice
import app.rope.android.data.MessageKind
import app.rope.android.data.RoleRules

data class HomeShortcut(
    val label: String,
    val destination: Screen,
)

/** In-app brand CTAs. No browser / landing / download buttons. */
object HomeCtas {
    val messenger = HomeShortcut("Перейти к мессенджеру", Screen.Chats)
    val status = HomeShortcut("К статусу", Screen.Status)
    val shortcuts: List<HomeShortcut> = listOf(messenger, status)
}

data class MainTab(
    val screen: Screen,
    val label: String,
)

object NavRules {
    /** Brand destination. Outside the messenger tab host. */
    val signedInRoot: Screen = Screen.Home

    /** Default tab after «Перейти к мессенджеру». */
    val messengerRoot: Screen = Screen.Chats

    val tabs: List<MainTab> = listOf(
        MainTab(Screen.Chats, "Чаты"),
        MainTab(Screen.Groups, "Группы"),
        MainTab(Screen.Calls, "Звонки"),
        MainTab(Screen.People, "Люди"),
        MainTab(Screen.Status, "Статус"),
    )

    fun isMessengerTab(screen: Screen): Boolean = tabs.any { it.screen == screen }

    fun isMessengerShell(screen: Screen): Boolean = when (screen) {
        Screen.Chats, Screen.Groups, Screen.Calls, Screen.People, Screen.Status,
        Screen.Chat, Screen.NewGroup, Screen.GroupInfo,
        -> true
        else -> false
    }

    fun showsBottomBar(screen: Screen, signedIn: Boolean): Boolean =
        signedIn && isMessengerTab(screen)

    fun canOpenHome(screen: Screen, signedIn: Boolean): Boolean =
        signedIn && screen != Screen.Home &&
            screen != Screen.Start &&
            screen != Screen.Provision &&
            screen != Screen.Join

    fun titleOpensHome(screen: Screen, signedIn: Boolean): Boolean = canOpenHome(screen, signedIn)

    /** Logo is the only home control — no «На главную» label. */
    fun showsHomeAction(screen: Screen, signedIn: Boolean): Boolean =
        InstantUi.showsHomeAction(screen, signedIn)

    fun showsInviteCta(role: String?): Boolean = RoleRules.canShowInviteQr(role)

    /** Shared AppBar never repeats the brand wordmark or the username. */
    fun chromeShowsWordmark(): Boolean = false

    fun chromeShowsUsername(): Boolean = false

    fun chromeShowsUserChip(signedIn: Boolean): Boolean = signedIn

    /**
     * Section title in the bar — «Чаты», «Статус», «офлайн».
     * Empty on Home: the hero already has the Rope wordmark.
     */
    fun chromeTitle(screen: Screen, offline: Boolean = false): String {
        if (offline) return "офлайн"
        return when (screen) {
            Screen.Chats -> "Чаты"
            Screen.Groups -> "Группы"
            Screen.NewGroup -> "Новая группа"
            Screen.GroupInfo -> "Группа"
            Screen.Calls -> "Звонки"
            Screen.People -> "Люди"
            Screen.Status -> "Статус"
            Screen.Settings -> "Настройки"
            Screen.Invite -> "Приглашение"
            Screen.Home, Screen.Start, Screen.Provision, Screen.Join, Screen.Chat -> ""
        }
    }

    /** Own display name belongs on Home, People, and Settings — not in every AppBar. */
    fun contentShowsOwnName(screen: Screen): Boolean =
        screen == Screen.Home || screen == Screen.People || screen == Screen.Settings

    fun selectedTab(screen: Screen): Screen? = when (screen) {
        Screen.Chats, Screen.Chat -> Screen.Chats
        Screen.Groups, Screen.NewGroup, Screen.GroupInfo -> Screen.Groups
        Screen.Calls -> Screen.Calls
        Screen.People -> Screen.People
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

    fun homePeopleHint(role: String?, people: List<DirectoryDevice>): String =
        if (people.isNotEmpty()) {
            people.take(4).joinToString(" · ") { it.displayName.ifBlank { it.deviceId.take(6) } }
        } else if (showsInviteCta(role)) {
            "Пока никого. Покажите QR — человек появится здесь."
        } else {
            "Пока никого. Когда организатор пригласит человека, он появится здесь."
        }

    fun refreshesLists(screen: Screen): Boolean =
        screen == Screen.Chats ||
            screen == Screen.Groups ||
            screen == Screen.Calls ||
            screen == Screen.Home ||
            screen == Screen.People
}
