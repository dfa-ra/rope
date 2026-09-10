package app.rope.android.data

/**
 * Telegram-like People list: the avatar opens the 1:1 profile; the rest of
 * the row still opens the chat. No LocalStore bump.
 */
object PeopleProfileRules {
    const val AVATAR = "Профиль"

    fun canOpen(deviceId: String?, selfId: String?): Boolean {
        val id = PeerIds.normalize(deviceId)
        return id.isNotBlank() && !PeerIds.same(id, selfId)
    }
}
