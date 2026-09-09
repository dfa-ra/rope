package app.rope.android

/**
 * Recents thumbnails and screenshots of these screens would capture
 * SSH passwords / PEMs / GitHub PATs (Provision, Status) or a live invite
 * token (Join). Invite QR stays shareable. Not envelope crypto.
 */
object SecureDisplayRules {
    fun lockRecents(screen: Screen): Boolean =
        screen == Screen.Provision || screen == Screen.Join || screen == Screen.Status
}
