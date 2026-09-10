package app.rope.android.data

/**
 * Owner Group info **Пригласить** opens the existing invite QR via createInvite().
 * Distinct from save-qr, rotate, TTL, and share-invite. Not a group-token API.
 */
object GroupInviteRules {
    const val LABEL = "Пригласить"

    fun canShow(role: String?): Boolean = RoleRules.canInvite(role)
}
