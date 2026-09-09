package app.rope.android

/**
 * Telegram-like chrome and “never block the shell” rules.
 * Tabs and chats paint from cache immediately; the network may catch up later.
 */
object InstantUi {
    /** Global Rope AppBar. Chat / group info / peer profile own their single header. */
    fun showsAppBar(screen: Screen): Boolean = when (screen) {
        Screen.Chat, Screen.GroupInfo, Screen.NewGroup, Screen.PeerProfile -> false
        else -> true
    }

    fun showsChromeBack(): Boolean = false

    fun showsHomeLabel(): Boolean = false

    fun showsHomeAction(screen: Screen, signedIn: Boolean): Boolean = false

    /** Full-screen splash / progress only for login and first install — never for tabs. */
    fun busyBlocksUi(screen: Screen): Boolean = when (screen) {
        Screen.Provision, Screen.Join, Screen.Start -> true
        else -> false
    }

    fun instantTransition(from: Screen, to: Screen): Boolean {
        if (from == to) return true
        val messengerHop =
            (NavRules.isMessengerTab(from) || from == Screen.Chat || from == Screen.GroupInfo ||
                from == Screen.NewGroup || from == Screen.PeerProfile || from == Screen.Archive) &&
                (NavRules.isMessengerTab(to) || to == Screen.Chat || to == Screen.GroupInfo ||
                    to == Screen.NewGroup || to == Screen.PeerProfile || to == Screen.Archive)
        return messengerHop
    }
}
