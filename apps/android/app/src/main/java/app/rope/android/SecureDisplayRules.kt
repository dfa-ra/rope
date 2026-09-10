package app.rope.android

/**
 * Recents thumbnails and screenshots of these screens would capture
 * SSH passwords / PEMs / GitHub PATs (Provision) or a live invite token (Join).
 * Optional Settings toggle also locks open chats. Invite QR stays shareable.
 * Not envelope crypto.
 */
object SecureDisplayRules {
    fun lockRecents(screen: Screen, hideChats: Boolean = false): Boolean {
        if (screen == Screen.Provision || screen == Screen.Join) return true
        if (!hideChats) return false
        return screen == Screen.Chat || screen == Screen.PeerProfile || screen == Screen.GroupInfo
    }
}
