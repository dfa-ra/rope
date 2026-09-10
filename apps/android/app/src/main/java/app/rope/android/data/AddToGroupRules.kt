package app.rope.android.data

/**
 * Telegram-like add-to-group from a 1:1 peer profile. Uses existing
 * group REST add-member. No LocalStore bump.
 */
object AddToGroupRules {
    const val SECTION = "Добавить в группу"

    fun canShow(peerId: String?, selfId: String?): Boolean {
        if (peerId.isNullOrBlank() || peerId == selfId) return false
        return !SavedMessagesRules.isSaved(peerId)
    }

    fun canAdd(g: RopeGroup, peerId: String?, selfId: String?, serverRole: String?): Boolean {
        if (!canShow(peerId, selfId) || selfId.isNullOrBlank()) return false
        val me = selfId
        val peer = peerId!!
        if (peer in g.members) return false
        val organizer = GroupChatUx.organizerId(g)
        return RoleRules.canManageGroupMembers(me in g.members, me, organizer, serverRole)
    }

    fun eligible(
        groups: List<RopeGroup>,
        peerId: String?,
        selfId: String?,
        serverRole: String?,
    ): List<RopeGroup> = groups
        .filter { canAdd(it, peerId, selfId, serverRole) }
        .sortedBy { it.name.lowercase() }

    fun confirm(name: String): String = "Точно в «$name»"

    fun notice(name: String): String = "Добавлен в «$name»"
}
