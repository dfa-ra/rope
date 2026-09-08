package app.rope.android.data

/** Owner-only Android controls. Guests never see these — hide, do not disable. */
object RoleRules {
    fun isOwner(role: String?): Boolean = role.equals("owner", ignoreCase = true)

    fun canInvite(role: String?): Boolean = isOwner(role)

    fun canShowInviteQr(role: String?): Boolean = canInvite(role)

    fun canUpgradeCore(role: String?): Boolean = isOwner(role)

    fun canShowAdminCards(role: String?): Boolean = isOwner(role)

    fun canEditServerSettings(role: String?): Boolean = isOwner(role)

    fun canWipeOrReinstall(role: String?): Boolean = isOwner(role)

    @Suppress("UNUSED_PARAMETER")
    fun canUpdateApp(role: String?): Boolean = true

    @Suppress("UNUSED_PARAMETER")
    fun canOpenStatus(role: String?): Boolean = true

    fun peopleEmptyHint(role: String?): String =
        if (canInvite(role)) {
            "Пока никого. Покажите QR — человек появится здесь."
        } else {
            "Пока никого. Когда организатор пригласит человека, он появится здесь."
        }

    fun peopleInviteAction(role: String?): String? = if (canInvite(role)) "Пригласить" else null

    fun callsEmptyBody(role: String?): String =
        if (canInvite(role)) {
            "Пригласите человека, откройте личный чат и нажмите трубку."
        } else {
            "Откройте личный чат и нажмите трубку."
        }

    fun chatsEmptyBody(role: String?): String =
        if (canInvite(role)) {
            "Пригласите человека QR-кодом или создайте группу."
        } else {
            "Когда появятся люди, здесь будут чаты. Можно создать группу."
        }

    fun groupNoMembersHint(role: String?): String =
        if (canInvite(role)) {
            "Пока некого добавить. Пригласите человека QR-кодом."
        } else {
            "Пока некого добавить. Попросите организатора пригласить человека."
        }

    /**
     * Add/remove others: organizer or server owner.
     * REST still accepts any member; the UI follows Telegram/WhatsApp.
     */
    fun canManageGroupMembers(
        isMember: Boolean,
        myId: String?,
        organizerId: String,
        serverRole: String?,
    ): Boolean {
        if (!isMember) return false
        if (isOwner(serverRole)) return true
        return !myId.isNullOrBlank() && myId == organizerId
    }

    fun canLeaveGroup(isMember: Boolean): Boolean = isMember
}
